

#include "toonz/scriptbinding_files.h"
#include <QScriptEngine>
#include <QFile>
#include <QFileInfo>
#include <QDirIterator>
#include <QScriptValueIterator>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QJsonParseError>
#include "tsystem.h"

namespace TScriptBinding {

//===========================================================================

FilePath::FilePath(const QString &filePath) : m_filePath(filePath) {}

FilePath::FilePath(const TFilePath &filePath)
    : m_filePath(QString::fromStdWString(filePath.getWideString())) {}

FilePath::~FilePath() {}

QScriptValue FilePath::ctor(QScriptContext *context, QScriptEngine *engine) {
  FilePath *file = new FilePath();
  if (context->argumentCount() == 1) {
    file->m_filePath = context->argument(0).toString();
  }
  return file->create(engine, file);
}

QScriptValue FilePath::toString() const { return tr("\"%1\"").arg(m_filePath); }

QString FilePath::getExtension() const {
  return QString::fromStdString(getToonzFilePath().getType());
}

QScriptValue FilePath::setExtension(const QString &extension) {
  TFilePath fp = getToonzFilePath().withType(extension.toStdString());
  m_filePath   = QString::fromStdWString(fp.getWideString());
  return context()->thisObject();
}

QString FilePath::getName() const {
  return QString::fromStdString(getToonzFilePath().getName());
}

void FilePath::setName(const QString &name) {
  TFilePath fp = getToonzFilePath().withName(name.toStdString());
  m_filePath   = QString::fromStdWString(fp.getWideString());
}

QScriptValue FilePath::getParentDirectory() const {
  FilePath *result = new FilePath(getToonzFilePath().getParentDir());
  return create(engine(), result);
}

void FilePath::setParentDirectory(const QScriptValue &folder) {
  TFilePath fp;
  QScriptValue err = checkFilePath(context(), folder, fp);
  if (!err.isError()) {
    m_filePath = QString::fromStdWString(
        getToonzFilePath().withParentDir(fp).getWideString());
  }
}

QScriptValue FilePath::withExtension(const QString &extension) {
  TFilePath fp = getToonzFilePath().withType(extension.toStdString());
  return create(engine(), new FilePath(fp));
}

QScriptValue FilePath::withName(const QString &extension) {
  TFilePath fp = getToonzFilePath().withName(extension.toStdString());
  return create(engine(), new FilePath(fp));
}

QScriptValue FilePath::withParentDirectory(
    const QScriptValue &parentDirectoryArg) {
  TFilePath parentDirectory;
  QScriptValue err =
      checkFilePath(context(), parentDirectoryArg, parentDirectory);
  if (err.isError())
    return err;
  else
    return create(
        engine(),
        new FilePath(getToonzFilePath().withParentDir(parentDirectory)));
}

bool FilePath::exists() const { return QFile(m_filePath).exists(); }

QDateTime FilePath::lastModified() const {
  return QFileInfo(m_filePath).lastModified();
}

TFilePath FilePath::getToonzFilePath() const {
  return TFilePath(m_filePath.toStdWString());
}

bool FilePath::isDirectory() const { return QFileInfo(m_filePath).isDir(); }

QScriptValue FilePath::concat(const QScriptValue &value) const {
  TFilePath fp;
  QScriptValue err;
  err = checkFilePath(context(), value, fp);
  if (err.isError()) return err;

  if (fp.isAbsolute())
    return context()->throwError(
        tr("can't concatenate an absolute path : %1").arg(value.toString()));
  fp = getToonzFilePath() + fp;
  return create(engine(), new FilePath(fp));
}

QScriptValue FilePath::files() const {
  if (!isDirectory()) {
    return context()->throwError(
        tr("%1 is not a directory").arg(toString().toString()));
  }
  TFilePathSet fpset;
  try {
    TSystem::readDirectory(fpset, getToonzFilePath());
    QScriptValue result = engine()->newArray();
    quint32 index       = 0;
    for (TFilePathSet::iterator it = fpset.begin(); it != fpset.end(); ++it) {
      FilePath *res = new FilePath(*it);
      result.setProperty(index++, res->create<FilePath>(engine()));
    }
    return result;
  } catch (...) {
    return context()->throwError(
        tr("can't read directory %1").arg(toString().toString()));
  }
}

namespace {

/*-- QtScript has no JSON bridge, so convert explicitly rather than evaluating
 * the document as source, which would execute whatever the file contains. --*/
QScriptValue jsonToScript(QScriptEngine *engine, const QJsonValue &value) {
  switch (value.type()) {
  case QJsonValue::Null:
    return QScriptValue(QScriptValue::NullValue);
  case QJsonValue::Bool:
    return QScriptValue(value.toBool());
  case QJsonValue::Double:
    return QScriptValue(value.toDouble());
  case QJsonValue::String:
    return QScriptValue(engine, value.toString());
  case QJsonValue::Array: {
    QJsonArray array   = value.toArray();
    QScriptValue result = engine->newArray(array.size());
    for (int i = 0; i < array.size(); i++)
      result.setProperty(i, jsonToScript(engine, array.at(i)));
    return result;
  }
  case QJsonValue::Object: {
    QJsonObject object  = value.toObject();
    QScriptValue result = engine->newObject();
    for (auto it = object.begin(); it != object.end(); ++it)
      result.setProperty(it.key(), jsonToScript(engine, it.value()));
    return result;
  }
  default:
    return QScriptValue(QScriptValue::UndefinedValue);
  }
}

QJsonValue scriptToJson(const QScriptValue &value) {
  if (value.isNull() || value.isUndefined()) return QJsonValue();
  if (value.isBool()) return QJsonValue(value.toBool());
  if (value.isNumber()) return QJsonValue(value.toNumber());
  if (value.isArray()) {
    QJsonArray array;
    int length = value.property("length").toInt32();
    for (int i = 0; i < length; i++)
      array.append(scriptToJson(value.property(i)));
    return array;
  }
  if (value.isObject()) {
    QJsonObject object;
    QScriptValueIterator it(value);
    while (it.hasNext()) {
      it.next();
      object.insert(it.name(), scriptToJson(it.value()));
    }
    return object;
  }
  return QJsonValue(value.toString());
}

}  // namespace

QScriptValue FilePath::readText() const {
  QFile file(m_filePath);
  if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
    return context()->throwError(
        tr("Can't read the file %1").arg(m_filePath));
  QString text = QString::fromUtf8(file.readAll());
  file.close();
  return QScriptValue(engine(), text);
}

QScriptValue FilePath::writeText(const QString &text) const {
  TFilePath fp = getToonzFilePath();
  try {
    TSystem::touchParentDir(fp);
  } catch (...) {
  }
  QFile file(m_filePath);
  if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate | QIODevice::Text))
    return context()->throwError(
        tr("Can't write the file %1").arg(m_filePath));
  if (file.write(text.toUtf8()) < 0) {
    file.close();
    return context()->throwError(
        tr("Can't write the file %1").arg(m_filePath));
  }
  file.close();
  return QScriptValue();
}

QScriptValue FilePath::readJson() const {
  QFile file(m_filePath);
  if (!file.open(QIODevice::ReadOnly))
    return context()->throwError(
        tr("Can't read the file %1").arg(m_filePath));
  QByteArray data = file.readAll();
  file.close();

  QJsonParseError error;
  QJsonDocument document = QJsonDocument::fromJson(data, &error);
  if (error.error != QJsonParseError::NoError)
    return context()->throwError(tr("%1 is not valid JSON: %2")
                                    .arg(m_filePath)
                                    .arg(error.errorString()));
  if (document.isArray()) return jsonToScript(engine(), document.array());
  return jsonToScript(engine(), document.object());
}

QScriptValue FilePath::writeJson(const QScriptValue &value) const {
  QJsonValue json = scriptToJson(value);
  QJsonDocument document;
  if (json.isArray())
    document = QJsonDocument(json.toArray());
  else if (json.isObject())
    document = QJsonDocument(json.toObject());
  else
    return context()->throwError(
        tr("Only objects and arrays can be written as JSON"));

  return writeText(QString::fromUtf8(document.toJson(QJsonDocument::Indented)));
}

QScriptValue checkFilePath(QScriptContext *context, const QScriptValue &value,
                           TFilePath &fp) {
  FilePath *filePath = qscriptvalue_cast<FilePath *>(value);
  if (filePath) {
    fp = filePath->getToonzFilePath();
  } else if (value.isString()) {
    fp = TFilePath(value.toString().toStdWString());
  } else {
    return context->throwError(
        QObject::tr("Argument doesn't look like a file path : %1")
            .arg(value.toString()));
  }
  return QScriptValue();
}

}  // namespace TScriptBinding
