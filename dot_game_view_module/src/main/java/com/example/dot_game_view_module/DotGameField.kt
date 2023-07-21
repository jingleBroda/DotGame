package com.example.dot_game_view_module

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//храним состояния ячеек

enum class Dot{
    PLAYER_1,
    PLAYER_2,
    EMPTY,
    EMPTY_CLOSED
}

//слушатель нажатия на ячейку с рисованием точки
typealias OnFieldChangeListener = (field:DotGameField)->Unit

class DotGameField(
    val rowsBattlefield:Int = 7,
    val columnsBattlefield:Int = 7,
    private val dots:Array<Array<Dot>>,
    saveCurrentDot:SaveCurrentDot?,
    val saveExistingLines:List<SaveLine>?
) {
    val rowsPoints = rowsBattlefield-1
    val columnsPoints = columnsBattlefield-1
    private var currentDotRow:Int = saveCurrentDot?.row ?: 0
    private var currentDotColumn:Int = saveCurrentDot?.column ?: 0
    val listener = mutableSetOf<OnFieldChangeListener>()

    constructor(rows:Int=7, columns:Int=7, dots:Array<Array<Dot>>, saveCurrentDot:SaveCurrentDot?):this(
        rows,
        columns,
        dots,
        saveCurrentDot,
        null
    )

    constructor(rows:Int=7, columns:Int=7, dots:Array<Array<Dot>>):this(
        rows,
        columns,
        dots,
        null
    )
    constructor(rows:Int=7, columns:Int=7):this(
        rows,
        columns,
        Array(rows-1){ Array(columns-1) { Dot.EMPTY } }
    )

    fun getDot(row:Int,column:Int):Dot =
        if(rowsPoints<0 || column<0 || row>=rowsPoints || column>=columnsPoints)  Dot.EMPTY
        else dots[row][column]

    fun setDot(row:Int,column:Int, dot:Dot){
        if(row<0 || column<0 || row>=rowsPoints || column>=columnsPoints) return
        if(getDot(row, column)!=dot){
            dots[row][column] = dot
            currentDotRow = row
            currentDotColumn = column
            if(dot != Dot.EMPTY_CLOSED) listener.forEach{ it?.invoke(this) }
        }
    }

    fun gameOverProgress():Boolean{
        var result = true
        for(row in dots.indices){
            for(column in dots[row].indices){
                if(dots[row][column] == Dot.EMPTY){
                    result = false
                    break
                }
            }
        }
        return result
    }

    fun getCurrentDotRow():Int = currentDotRow
    fun getCurrentDotColumn():Int = currentDotColumn

    fun saveState(saveLines: List<SaveLine>):Memento = Memento(
        rowsBattlefield,
        columnsBattlefield,
        dots,
        SaveCurrentDot(currentDotRow, currentDotColumn),
        saveLines
    )

    @Parcelize
    data class SaveLine(
        val startDotRow:Int,
        val startDotColumn:Int,
        val endDotRow:Int,
        val endDotColumn:Int,
    ): Parcelable

    @Parcelize
    data class SaveCurrentDot(
        val row:Int,
        val column:Int,
    ): Parcelable

    @Suppress("ArrayInDataClass")
    @Parcelize
    data class Memento(
        private val rows: Int,
        private val columns: Int,
        private val dots: Array<Array<Dot>>,
        private val saveCurrentDot:SaveCurrentDot,
        private val lines: List<SaveLine>
    ) : Parcelable {
        fun restoreField(): DotGameField {
            return DotGameField(rows, columns, dots, saveCurrentDot, lines)
        }
    }
}