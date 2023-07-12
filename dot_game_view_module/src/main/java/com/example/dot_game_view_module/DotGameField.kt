package com.example.dot_game_view_module

import android.util.Log

//храним состояния ячеек

enum class Dot{
    PLAYER_1,
    PLAYER_2,
    EMPTY
}

//слушатель нажатия на ячейку с рисованием точки
typealias OnFieldChangeListener = (field:DotGameField)->Unit

class DotGameField(
    val rowsBattlefield:Int = 7,
    val columnsBattlefield:Int = 7
) {
    val rowsPoints = rowsBattlefield-1
    val columnsPoints = columnsBattlefield-1
    private var currentDotRow:Int = 0
    private var currentDotColumn:Int = 0
    val listener = mutableSetOf<OnFieldChangeListener>()
    private val dots = Array(rowsPoints){
        Array(columnsPoints) {
            Dot.EMPTY
        }
    }

    fun getDot(row:Int,column:Int):Dot =
        if(rowsPoints<0 || column<0 || row>=rowsPoints || column>=columnsPoints)  Dot.EMPTY
        else dots[row][column]

    fun setDot(row:Int,column:Int, dot:Dot){
        if(row<0 || column<0 || row>=rowsPoints || column>=columnsPoints) return
        if(getDot(row, column)!=dot){
            dots[row][column] = dot
            currentDotRow = row
            currentDotColumn = column
            listener.forEach{ it?.invoke(this) }
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
}