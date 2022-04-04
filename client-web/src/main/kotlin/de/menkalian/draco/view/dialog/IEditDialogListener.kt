package de.menkalian.draco.view.dialog

interface IEditDialogListener<T> {
    fun onSaved(obj: T): Boolean
    fun onCancelled()
}