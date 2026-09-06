package com.animame.editor

/** Lightweight command history shared by drawing, timeline and layer operations. */
interface EditorCommand { fun undo(); fun redo() }

class EditorHistory(private val capacity:Int=100) {
    private val undoStack=ArrayDeque<EditorCommand>()
    private val redoStack=ArrayDeque<EditorCommand>()
    fun execute(command:EditorCommand){command.redo();undoStack.addLast(command);redoStack.clear();trim()}
    fun undo(){if(undoStack.isEmpty())return;val c=undoStack.removeLast();c.undo();redoStack.addLast(c)}
    fun redo(){if(redoStack.isEmpty())return;val c=redoStack.removeLast();c.redo();undoStack.addLast(c);trim()}
    fun clear(){undoStack.clear();redoStack.clear()}
    val canUndo:Boolean get()=undoStack.isNotEmpty()
    val canRedo:Boolean get()=redoStack.isNotEmpty()
    private fun trim(){while(undoStack.size>capacity)undoStack.removeFirst()}
}

class LambdaCommand(private val doAction:()->Unit,private val undoAction:()->Unit):EditorCommand{
    override fun redo()=doAction()
    override fun undo()=undoAction()
}
