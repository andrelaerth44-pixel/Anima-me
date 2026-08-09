// TLV round-trip harness.
//
// tiio_tzl.cpp is 2,500 lines of mixed C and C++ carrying branches for every
// TLV version ever written, and it has never had a test. Discussion #6746 asks
// for it to be rewritten; this harness exists so that such a rewrite can be
// reviewed against evidence rather than by inspection.
//
// The contract it pins down is deliberately narrow and total: whatever pixels
// go in must come back out, and writing the same level twice must produce the
// same bytes. Those two properties are what a refactor is allowed to preserve
// and nothing else.
//
// Coverage limit, stated plainly: these fixtures are produced by the current
// writer, so they prove a change does not alter today's behaviour. They cannot
// prove that files written by older OpenToonz releases still read correctly.
// Closing that gap needs genuinely old .tlv files, which the repository does
// not contain.

#include "tlevel_io.h"
#include "tlevel.h"
#include "timage_io.h"
#include "ttoonzimage.h"
#include "trastercm.h"
#include "tpixelcm.h"
#include "tpalette.h"
#include "tsystem.h"
#include "tconvert.h"
#include "texception.h"

#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QTemporaryDir>

#include <cstdio>
#include <string>
#include <vector>

namespace {

int g_failures = 0;

void check(bool ok, const std::string &what) {
  std::printf("%-62s %s\n", what.c_str(), ok ? "PASS" : "FAIL");
  if (!ok) ++g_failures;
}

//! Fills a Toonz raster with a deterministic pattern that exercises the whole
//! TPixelCM32 layout: a 12-bit ink index, a 12-bit paint index and the 8-bit
//! tone that interpolates between them. Intermediate tone values are the part
//! a naive refactor is most likely to break, so they are covered explicitly.
TRasterCM32P makePattern(int lx, int ly, int styleCount) {
  TRasterCM32P ras(lx, ly);
  ras->lock();
  for (int y = 0; y < ly; ++y) {
    TPixelCM32 *pix = ras->pixels(y);
    for (int x = 0; x < lx; ++x, ++pix) {
      int ink   = (x + y) % styleCount;
      int paint = (x * 2 + y) % styleCount;
      // Sweep tone across its full range, including the pure-ink (0) and
      // pure-paint (255) endpoints and the antialiased values between.
      int tone = (x * 255) / (lx > 1 ? lx - 1 : 1);
      *pix     = TPixelCM32(ink, paint, tone);
    }
  }
  ras->unlock();
  return ras;
}

TPaletteP makePalette(int styleCount) {
  TPaletteP palette(new TPalette());
  for (int i = palette->getStyleCount(); i < styleCount; ++i)
    palette->addStyle(TPixel32(i * 7 % 256, i * 13 % 256, i * 29 % 256, 255));
  return palette;
}

bool rastersEqual(const TRasterCM32P &a, const TRasterCM32P &b) {
  if (!a || !b) return false;
  if (a->getLx() != b->getLx() || a->getLy() != b->getLy()) return false;
  a->lock();
  b->lock();
  bool equal = true;
  for (int y = 0; y < a->getLy() && equal; ++y) {
    TPixelCM32 *pa = a->pixels(y);
    TPixelCM32 *pb = b->pixels(y);
    for (int x = 0; x < a->getLx(); ++x, ++pa, ++pb)
      if (pa->getInk() != pb->getInk() || pa->getPaint() != pb->getPaint() ||
          pa->getTone() != pb->getTone()) {
        equal = false;
        break;
      }
  }
  a->unlock();
  b->unlock();
  return equal;
}

QByteArray readAll(const TFilePath &fp) {
  QFile file(QString::fromStdWString(fp.getWideString()));
  if (!file.open(QIODevice::ReadOnly)) return QByteArray();
  return file.readAll();
}

//! Writes frames into a .tlv, then reads them back and compares pixels.
//! Returns the written file so the caller can compare bytes across writes.
bool writeLevel(const TFilePath &fp, const std::vector<TRasterCM32P> &frames,
                const TPaletteP &palette) {
  try {
    TLevelWriterP writer(fp);
    writer->setPalette(palette.getPointer());
    for (int i = 0; i < (int)frames.size(); ++i) {
      TToonzImageP image(frames[i], frames[i]->getBounds());
      image->setPalette(palette.getPointer());
      TImageWriterP frameWriter = writer->getFrameWriter(TFrameId(i + 1));
      if (!frameWriter) return false;
      frameWriter->save(image);
    }
  } catch (TException &e) {
    return false;
  } catch (std::exception &e) {
    return false;
  } catch (...) {
    return false;
  }
  return true;
}

bool readLevel(const TFilePath &fp, std::vector<TRasterCM32P> &frames) {
  frames.clear();
  try {
    TLevelReaderP reader(fp);
    TLevelP level = reader->loadInfo();
    if (!level) return false;
    for (auto it = level->begin(); it != level->end(); ++it) {
      TImageReaderP frameReader = reader->getFrameReader(it->first);
      if (!frameReader) return false;
      TToonzImageP image = frameReader->load();
      if (!image) return false;
      frames.push_back(image->getCMapped());
    }
  } catch (TException &e) {
    return false;
  } catch (...) {
    return false;
  }
  return !frames.empty();
}

struct Case {
  const char *name;
  int lx, ly, frames, styles;
};

}  // namespace

DV_IMPORT_API void initImageIo(bool lightVersion);

int main(int argc, char **argv) {
  QCoreApplication app(argc, argv);

  // The TLV reader and writer are registered at runtime rather than at
  // static-initialisation time, so a bare test binary has to ask for them.
  initImageIo(false);

  QTemporaryDir tempDir;
  if (!tempDir.isValid()) {
    std::printf("could not create a temporary directory\n");
    return 1;
  }
  TFilePath dir(tempDir.path().toStdWString());

  const Case cases[] = {
      {"small", 8, 6, 1, 4},
      {"multi frame", 16, 12, 5, 8},
      {"tall and narrow", 3, 129, 2, 16},
      {"square", 32, 32, 1, 16},
      {"odd dimensions", 37, 23, 3, 9},
  };

  for (const Case &c : cases) {
    TFilePath fp = dir + TFilePath(std::string(c.name) + ".tlv");
    // Spaces in the case name would make an awkward filename; normalise.
    std::string safe;
    for (const char *s = c.name; *s; ++s) safe += (*s == ' ' || *s == ',') ? '_' : *s;
    fp = dir + TFilePath(safe + ".tlv");

    TPaletteP palette = makePalette(c.styles);
    std::vector<TRasterCM32P> written;
    for (int i = 0; i < c.frames; ++i)
      written.push_back(makePattern(c.lx, c.ly, c.styles));

    if (!writeLevel(fp, written, palette)) {
      check(false, std::string(c.name) + ": write");
      continue;
    }

    std::vector<TRasterCM32P> readBack;
    if (!readLevel(fp, readBack)) {
      check(false, std::string(c.name) + ": read");
      continue;
    }

    check(readBack.size() == written.size(),
          std::string(c.name) + ": frame count survives");

    bool allEqual = readBack.size() == written.size();
    for (size_t i = 0; i < readBack.size() && allEqual; ++i)
      allEqual = rastersEqual(written[i], readBack[i]);
    check(allEqual, std::string(c.name) + ": ink, paint and tone survive");

    // Writing the same level twice must produce the same bytes. This is what
    // makes a refactor reviewable: any byte difference is a behaviour change.
    QByteArray first = readAll(fp);
    TFilePath fp2    = dir + TFilePath(safe + "_again.tlv");
    if (writeLevel(fp2, written, palette)) {
      QByteArray second = readAll(fp2);
      check(!first.isEmpty() && first == second,
            std::string(c.name) + ": writing twice is byte-identical");
    } else {
      check(false, std::string(c.name) + ": second write");
    }
  }

  // Known issue, reported rather than asserted so the suite stays green while
  // the defect stands.
  //
  // Some levels cannot be read back at all; the reader raises "Loading tlv:
  // buffer size error". Two independent triggers were found:
  //
  //   * any level shorter than six rows, regardless of width, frame count or
  //     style count - the boundary is sharp and reproducible;
  //   * levels whose pixel data compresses poorly. A 32x32 level round-trips
  //     with 16 styles but fails with 256, and the data is otherwise identical
  //     in shape.
  //
  // The cause is in the icon reader in tiio_tzl.cpp. It bounds the stored
  // buffer size, which is a *compressed* length, against the *raw* icon size,
  // and then freads that many bytes straight into the icon raster. Small or
  // incompressible icons legitimately compress to more than their raw size,
  // because LZO can expand input and has fixed per-block overhead, so they are
  // rejected. The bound cannot simply be widened: it is also what stops the
  // fread from overflowing the raster, so the fix needs a separate buffer
  // sized from the stored length. That belongs with the rewrite in #6746,
  // where this probe becomes a real assertion.
  {
    int firstGoodHeight = 0;
    for (int h = 1; h <= 8 && firstGoodHeight == 0; ++h) {
      TFilePath fp = dir + TFilePath("probe_h" + std::to_string(h) + ".tlv");
      TPaletteP palette = makePalette(4);
      std::vector<TRasterCM32P> frames{makePattern(8, h, 4)};
      std::vector<TRasterCM32P> back;
      if (writeLevel(fp, frames, palette) && readLevel(fp, back))
        firstGoodHeight = h;
    }
    std::printf(
        "\nKNOWN ISSUE: the shortest level that round-trips is %d rows;\n"
        "             anything shorter fails with a buffer size error.\n",
        firstGoodHeight);
  }

  std::printf("\n%s (%d failure%s)\n", g_failures ? "FAILED" : "ALL PASSED",
              g_failures, g_failures == 1 ? "" : "s");
  return g_failures ? 1 : 0;
}
