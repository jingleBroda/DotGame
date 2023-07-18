package com.example.dot_game_view_module

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.lang.Float.min
import java.lang.Integer.max
import kotlin.properties.Delegates

typealias OnPointActionListener = (row:Int, column:Int, field:DotGameField)->Unit
typealias OnCaptureCounterListener = (color:Dot, numbCounter:Int)->Unit

class DotGameView(
    context: Context,
    attrSet:AttributeSet?,
    defStyleAttr:Int,
    defStyleRes:Int
):View(context, attrSet, defStyleAttr, defStyleRes){

    private enum class PaintSelectionType{
        Line,
        Point
    }

    private data class DotPixels(
        val row:Int,
        val column:Int,
        val coordinatesX:Float,
        val coordinatesY:Float,
        val ownerPlayer:Dot
    )

    private data class Line(
        val dotStart:DotPixels,
        val dotEnd:DotPixels,
        val ownerPlayer:Dot
    )

    var dotGameField:DotGameField? = null
        set(value){
            field?.listener?.remove(listener)
            field = value
            arrayDotPixels = Array(field?.rowsPoints?:0){
                Array(field?.columnsPoints?:0){
                    null
                }
            }
            field?.listener?.add(listener)
            updateViewSize()
            requestLayout()
            invalidate()
        }
    private var arrayDotPixels:Array<Array<DotPixels?>> = Array(dotGameField?.rowsPoints?:0){
        Array(dotGameField?.columnsPoints?:0){
            null
        }
    }
    private val listLine = mutableListOf<Line>()
    private var setPolygons = mutableSetOf<Set<DotPixels>>()
    var actionListener:OnPointActionListener? = null
    var captureCounterListener:OnCaptureCounterListener? = null

    private var player1Color by Delegates.notNull<Int>()
    private var player2Color by Delegates.notNull<Int>()
    private var gridColor by Delegates.notNull<Int>()

    private val fieldRect = RectF()
    private val pointRect = RectF()
    private var cellSize = 0f
    //паддинг для рисования крестиков и ноликов, мб от этого надо удет отказаться
    private var cellPadding = 0f

    private lateinit var gridPaint:Paint
    private lateinit var battlefieldPaint:Paint
    private lateinit var player1DotPaint:Paint
    private lateinit var player2DotPaint:Paint
    private lateinit var player1LinePaint:Paint
    private lateinit var player2LinePaint:Paint

    constructor(context: Context,attrSet:AttributeSet?, defStyleAttr:Int):
            this(context, attrSet, defStyleAttr,R.style.GlobalDotGameStyle)
    constructor(context: Context, attrSet: AttributeSet?):
            this(context, attrSet,R.attr.dotGameFieldStyle)
    constructor(context: Context):
            this(context, null)
    init{
        if(attrSet!=null){
            initAttr(attrSet, defStyleAttr, defStyleRes)
        }
        else{
            initDefaultAttr()
        }
        initPaints()
        if(isInEditMode){
            dotGameField = DotGameField()
            dotGameField?.setDot(0,0,Dot.PLAYER_2)
            dotGameField?.setDot(0,1,Dot.PLAYER_1)
            dotGameField?.setDot(1,1,Dot.PLAYER_2)
            dotGameField?.setDot(1,0,Dot.PLAYER_1)
        }
    }

    private fun initPaints(){
        battlefieldPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        battlefieldPaint.color = DEFAULT_BATTLEFIELD_COLOR
        battlefieldPaint.style = Paint.Style.STROKE
        battlefieldPaint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics
        )

        gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        gridPaint.color = gridColor
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics
        )

        player1LinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        player1LinePaint.color = player1Color
        player1LinePaint.style = Paint.Style.STROKE
        player1LinePaint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics
        )

        player2LinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        player2LinePaint.color = player2Color
        player2LinePaint.style = Paint.Style.STROKE
        player2LinePaint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics
        )

        player1DotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        player1DotPaint.color = player1Color
        player1DotPaint.style = Paint.Style.FILL

        player2DotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        player2DotPaint.color = player2Color
        player2DotPaint.style = Paint.Style.FILL
    }

    private fun paintSelection(dot:Dot, paintType:PaintSelectionType):Paint =
        when(paintType){
            PaintSelectionType.Line-> if(dot == Dot.PLAYER_1) player1LinePaint else player2LinePaint
            PaintSelectionType.Point-> if(dot == Dot.PLAYER_1) player1DotPaint else player2DotPaint
        }

    private fun initAttr(attrSet:AttributeSet?, defStyleAttr:Int, defStyleRes:Int){
        val typedArray = context.obtainStyledAttributes(
            attrSet,
            R.styleable.DotGameView,
            defStyleAttr,
            defStyleRes
        )

        player1Color = typedArray.getColor(R.styleable.DotGameView_player1Color, DEFAULT_PLAYER_1_COLOR)
        player2Color = typedArray.getColor(R.styleable.DotGameView_player2Color, DEFAULT_PLAYER_2_COLOR)
        gridColor = typedArray.getColor(R.styleable.DotGameView_gridDotGameColor, DEFAULT_GRID_COLOR)

        typedArray.recycle()
    }

    private fun initDefaultAttr(){
        player1Color = DEFAULT_PLAYER_1_COLOR
        player2Color = DEFAULT_PLAYER_2_COLOR
        gridColor = DEFAULT_GRID_COLOR
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dotGameField?.listener?.add(listener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dotGameField?.listener?.remove(listener)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSize()
    }

    private fun updateViewSize(){
        val field = this.dotGameField ?: return

        val safeWith = width - paddingLeft - paddingRight
        val safeHeight = height - paddingTop - paddingBottom

        val cellWith = safeWith / field.columnsBattlefield.toFloat()
        val cellHeight = safeHeight / field.rowsBattlefield.toFloat()
        cellSize = min(cellWith, cellHeight)
        cellPadding = cellSize * 0.2f

        val fieldWith = cellSize * field.columnsBattlefield
        val fieldHeight = cellSize * field.rowsBattlefield

        fieldRect.left = paddingLeft + (safeWith - fieldWith) / 2
        fieldRect.top = paddingTop + (safeHeight - fieldHeight) / 2
        fieldRect.right = fieldRect.left + fieldWith
        fieldRect.bottom = fieldRect.top + fieldHeight

        pointRect.left = fieldRect.left + cellSize/2
        pointRect.top = fieldRect.top + cellSize/2
        pointRect.right = fieldRect.right - cellSize/2
        pointRect.bottom = fieldRect.bottom - cellSize/2
    }

    //договариваемся о размере с компановщиком
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWith = suggestedMinimumWidth + paddingLeft + paddingRight
        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        val desiredCellSizeInPixel = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DESIRED_CELL_SIZE,
            resources.displayMetrics
        ).toInt()
        val rows = dotGameField?.rowsBattlefield ?:0
        val columns = dotGameField?.columnsBattlefield ?:0

        val desiredWith = max(minWith, columns * desiredCellSizeInPixel + paddingLeft + paddingRight)
        val desiredHeight = max(minHeight, rows * desiredCellSizeInPixel + paddingTop + paddingBottom)

        setMeasuredDimension(
            resolveSize(desiredWith, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(dotGameField == null) return
        if(cellSize == 0f) return
        if(fieldRect.width()<=0) return
        if(fieldRect.height()<=0) return

        drawGrid(canvas)
        drawBattlefieldRect(canvas)
        drawPoints(canvas)
        drawDotConnection(canvas)
        checkClosedLoop()
        mergePolygons(setPolygons)
        searchEnemyDotInPolygons()
        Log.d("testFindPolygons", "$setPolygons")
    }

    private fun drawGrid(canvas: Canvas){
        val field = this.dotGameField ?: return

        val xStart = fieldRect.left
        val xEnd = fieldRect.right
        for(i in 0..field.rowsBattlefield){
            val y = fieldRect.top + cellSize*i
            canvas.drawLine(xStart, y, xEnd, y, gridPaint)
        }

        val yStart = fieldRect.top
        val yEnd = fieldRect.bottom
        for(i in 0..field.columnsBattlefield){
            val x = fieldRect.left + cellSize * i
            canvas.drawLine(x, yStart, x, yEnd, gridPaint)
        }
    }

    private fun drawBattlefieldRect(canvas:Canvas){
        //левая сторона
        canvas.drawLine(
            pointRect.left,
            pointRect.top,
            pointRect.left,
            pointRect.bottom,
            battlefieldPaint
        )
        //верх
        canvas.drawLine(
            pointRect.left,
            pointRect.top,
            pointRect.right,
            pointRect.top,
            battlefieldPaint
        )
        canvas.drawLine(
            pointRect.right,
            pointRect.top,
            pointRect.right,
            pointRect.bottom,
            battlefieldPaint
        )
        canvas.drawLine(
            pointRect.right,
            pointRect.bottom,
            pointRect.left,
            pointRect.bottom,
            battlefieldPaint
        )
    }

    private fun drawPoints(canvas:Canvas){
        val field = this.dotGameField ?: return

        val firstPointX = fieldRect.left+cellSize
        val firstPointY = fieldRect.top+cellSize

        for(row in 0 until field.rowsPoints){
            val currentPointY = firstPointY+(cellSize*row)
            for(colum in 0 until field.columnsPoints){
                val currentPointX = firstPointX+(cellSize*colum)
                if(field.getDot(row, colum) != Dot.EMPTY && field.getDot(row, colum) != Dot.EMPTY_CLOSED){
                    arrayDotPixels[row][colum] = DotPixels(
                        row,
                        colum,
                        currentPointX,
                        currentPointY,
                        field.getDot(row, colum)
                    )

                    canvas.drawCircle(
                        currentPointX,
                        currentPointY,
                        RADIUS_POINT,
                        paintSelection(field.getDot(row, colum), PaintSelectionType.Point)
                    )
                }
            }
        }
    }

    private fun drawDotConnection(canvas:Canvas){
        val field = this.dotGameField ?: return
        val current:DotPixels = arrayDotPixels[field.getCurrentDotRow()][field.getCurrentDotColumn()] ?: return
        //рисуем существующие линии
        drawOldLine(canvas)
        //ищем новые соединительные линии
        searchAndDrawNewLine(canvas,field,current)
    }

    private fun drawOldLine(canvas:Canvas){
        listLine.forEach { line->
            canvas.drawLine(
                line.dotStart.coordinatesX,
                line.dotStart.coordinatesY,
                line.dotEnd.coordinatesX,
                line.dotEnd.coordinatesY,
                paintSelection(line.ownerPlayer, PaintSelectionType.Line)
            )
        }
    }

    private fun searchAndDrawNewLine(
        canvas: Canvas,
        field:DotGameField,
        current: DotPixels
    ){
        //ищем стартовый индекс для правильного обхода соседних точек
        val startRow =
            if(current.row-1 >=0) current.row-1  else 0
        val startColumn =
            if(current.column-1 >=0) current.column-1 else 0
        val endRow =
            if(current.row+1 < field.rowsPoints) current.row+1  else field.rowsPoints-1
        val endColumn =
            if(current.column+1 < field.columnsPoints) current.column+1  else field.columnsPoints-1

        for(row in startRow..endRow){
            for(column in startColumn..endColumn){
                val sosed = arrayDotPixels[row][column]
                //делаем серию проверок перед отрисовкой
                if(sosed != null && sosed != current){
                    if(sosed.ownerPlayer==current.ownerPlayer){
                        if(!intersectionDiagonalLines(sosed, current)){
                            canvas.drawLine(
                                sosed.coordinatesX,
                                sosed.coordinatesY,
                                current.coordinatesX,
                                current.coordinatesY,
                                paintSelection(current.ownerPlayer, PaintSelectionType.Line)
                            )
                            listLine.add(Line(
                                sosed,
                                current,
                                current.ownerPlayer
                            ))
                        }
                    }
                }
            }
        }
    }

    private fun intersectionDiagonalLines(sosed: DotPixels, current:DotPixels):Boolean{
        val vershina:DotPixels?
        val nizina:DotPixels?

        if((sosed.row != current.row) && (sosed.column != current.column)){
            if(sosed.row<current.row){
                vershina = sosed
                nizina = current
            } else{
                vershina = current
                nizina = sosed
            }
        }
        else return false

        if(nizina.column<vershina.column){
            //значит направление параллельное побочной диагонали
            val oppositeVershina = DotPixels(
                vershina.row,
                vershina.column-1,
                vershina.coordinatesX,
                vershina.coordinatesY,
                vershina.ownerPlayer,
            )
            val oppositeNizina = DotPixels(
                nizina.row,
                nizina.column+1,
                nizina.coordinatesX,
                nizina.coordinatesY,
                nizina.ownerPlayer,
            )
            val oppositeLine = Line(
                oppositeVershina,
                oppositeNizina,
                vershina.ownerPlayer
            )
            return checkExistenceOppositeLine(oppositeLine)
        } else {
            //значит направление параллельное главной диагонали
            val oppositeVershina = DotPixels(
                vershina.row,
                vershina.column+1,
                vershina.coordinatesX,
                vershina.coordinatesY,
                vershina.ownerPlayer,
            )
            val oppositeNizina = DotPixels(
                nizina.row,
                nizina.column-1,
                nizina.coordinatesX,
                nizina.coordinatesY,
                nizina.ownerPlayer,
            )
            val oppositeLine = Line(
                oppositeVershina,
                oppositeNizina,
                vershina.ownerPlayer
            )
            return checkExistenceOppositeLine(oppositeLine)
        }
    }

    private fun checkExistenceOppositeLine(line:Line):Boolean{
        //если такой линии нет, то возвращаем false
        var result = false
        for(i in listLine){
            if(
                ((i.dotStart.row == line.dotStart.row &&
                 i.dotStart.column == line.dotStart.column) &&
                (i.dotEnd.row == line.dotEnd.row &&
                 i.dotEnd.column == line.dotEnd.column))

                ||

                ((i.dotStart.row == line.dotEnd.row &&
                  i.dotStart.column == line.dotEnd.column) &&
                 (i.dotEnd.row == line.dotStart.row &&
                  i.dotEnd.column == line.dotStart.column))
            ){
                result = true
                break
            }
        }

        return result
    }

    private fun checkClosedLoop(){
        val polygons = mutableSetOf<Set<DotPixels>>()
        val visited = mutableSetOf<Line>()

        listLine.forEach { line->
            val polygon = mutableSetOf<DotPixels>()
            polygon.add(line.dotStart)
            polygon.add(line.dotEnd)
            visited.add(line)
            val startDot = line.dotStart
            var currentDot = line.dotEnd

            while(true){
                val nextLine = listLine.firstOrNull{
                    (it.dotStart == currentDot || it.dotEnd == currentDot) && !visited.contains(it)
                }

                if(nextLine != null){
                    val prStop1 = nextLine.dotStart == startDot
                    val prStop2 = nextLine.dotEnd == startDot
                    if(prStop1 || prStop2){
                        break
                    }
                    else{
                        val lineDotStartAddPolygon = nextLine.dotStart == currentDot
                        currentDot = if(lineDotStartAddPolygon){
                            polygon.add(nextLine.dotEnd)
                            nextLine.dotEnd
                        } else{
                            polygon.add(nextLine.dotStart)
                            nextLine.dotStart
                        }
                        visited.add(nextLine)
                    }
                }
                else{
                    polygon.remove(polygon.last())
                    if(polygon.isEmpty()) break else currentDot = polygon.last()
                }

            }

            if(polygon.size>3){
                val deletedPolygons = inPolygonAbsorbsTheExisting(polygon, polygons)
                if(deletedPolygons.isNotEmpty()){
                    deletedPolygons.forEach{ deletedPolygon->
                        polygons.remove(deletedPolygon)
                    }
                }
                polygons.add(polygon)
            }

            visited.clear()
        }
        setPolygons = polygons
    }

    //находим все полигоны, которые входят в переданный сюда полигон. Возвращаем их список и удаляем их.
    private fun inPolygonAbsorbsTheExisting(insidePolygon:Set<DotPixels>, polygons:Set<Set<DotPixels>>):Set<Set<DotPixels>>{
        val result = mutableSetOf<Set<DotPixels>>()
        if(polygons.isEmpty()) return result
        polygons.forEach { existingPolygon ->
            var numberDot = 0
            for (dot in insidePolygon) {
                for (existingPolygonDot in existingPolygon) {
                    if (existingPolygonDot == dot) {
                        numberDot++
                        break
                    }
                }
            }
            if(numberDot == existingPolygon.size) result.add(existingPolygon)
        }
        return result
    }

    //слияние полигонов с общими точками
    private fun mergePolygons(polygons:MutableSet<Set<DotPixels>>){
        if(polygons.isEmpty()) return

        val mergingPolygons = mutableListOf<Set<Set<DotPixels>>>()
        polygons.forEach { firstPolygon->
            val mergingPolygonsElement = mutableSetOf<Set<DotPixels>>()
            var firstPolygonIncludeSecondPolygon = false
            for(secondPolygon in polygons){
                if(firstPolygon != secondPolygon){
                    for(firstPolygonDot in firstPolygon){
                        for(secondPolygonDot in secondPolygon){
                            if(firstPolygonDot == secondPolygonDot){
                                firstPolygonIncludeSecondPolygon = true
                            }
                            if(firstPolygonIncludeSecondPolygon) break
                        }
                        if(firstPolygonIncludeSecondPolygon) break
                    }
                    if(firstPolygonIncludeSecondPolygon){
                        mergingPolygonsElement.add(firstPolygon)
                        mergingPolygonsElement.add(secondPolygon)
                        break
                    }
                }
            }
            if(mergingPolygonsElement.isNotEmpty()) mergingPolygons.add(mergingPolygonsElement)
        }

        if(mergingPolygons.isNotEmpty()){
            val newPolygons = mutableSetOf<Set<DotPixels>>()
            mergingPolygons.forEach {
                val newPolygon = mutableSetOf<DotPixels>()
                it.forEach{ set->
                    setPolygons.remove(set)
                    set.forEach{ dot->
                        newPolygon.add(dot)
                    }
                }
                setPolygons.add(newPolygon)
                newPolygons.add(newPolygon)
            }
            mergePolygons(newPolygons)
        }
        else{
            polygons.forEach{
                setPolygons.add(it)
            }
        }
    }

    private fun searchEnemyDotInPolygons(){
        val field = this.dotGameField ?: return
        if(setPolygons.isEmpty()) return
        var currentNumbEnemyDot = 0
        var numbEnemyDotPlayer1 = 0
        var numbEnemyDotPlayer2 = 0
        var color: Dot

        setPolygons.forEach { polygon->
            color = polygon.first().ownerPlayer
            //сортируем полигон, чтобы проще находить потенциальные точки полигона для окружения
            val sortPolygon = polygon.sortedWith(compareBy<DotPixels>{it.row}.thenBy { it.column })
            //находим область точек, которую будем проверять
            var minRow =  field.rowsPoints
            var minColumn =  field.columnsPoints
            var maxRow = -1
            var maxColumn = -1
            sortPolygon.forEach { dotPolygon->
                if(dotPolygon.row > maxRow) maxRow = dotPolygon.row
                if(dotPolygon.column > maxColumn) maxColumn = dotPolygon.column
                if(dotPolygon.row < minRow) minRow = dotPolygon.row
                if(dotPolygon.column < minColumn) minColumn = dotPolygon.column
            }
            //начинаем проходить все точки в найденной области
            for(currentRow in minRow..maxRow){
                for(currentColumn in minColumn..maxColumn){
                    val currentDotColor = field.getDot(currentRow, currentColumn)

                    if(currentDotColor != Dot.EMPTY) {
                        arrayDotPixels[currentRow][currentColumn] ?: continue
                        //если точка вражеская, проверяем окружена ли она полигоном
                        if(
                            currentDotColor != color &&
                            checkClosedDot(sortPolygon, currentRow, currentColumn)
                        ){
                            currentNumbEnemyDot++
                        }
                    }
                    else{
                        if(checkClosedDot(sortPolygon, currentRow, currentColumn)){
                            field.setDot(currentRow, currentColumn, Dot.EMPTY_CLOSED)
                        }
                    }
                }
            }
            if(color == Dot.PLAYER_1) numbEnemyDotPlayer1 += currentNumbEnemyDot
            if(color == Dot.PLAYER_2) numbEnemyDotPlayer2 += currentNumbEnemyDot
            currentNumbEnemyDot = 0
        }
        captureCounterListener?.invoke(Dot.PLAYER_1, numbEnemyDotPlayer1)
        captureCounterListener?.invoke(Dot.PLAYER_2, numbEnemyDotPlayer2)
    }

    private fun checkClosedDot(
        sortPolygon:List<DotPixels>,
        currentRow:Int,
        currentColumn:Int,
    ):Boolean{
        //если точка подходит под определение вражеской:
        //ищем точки полигона, которые потенциально могут ограничить точку противника
        val dot1Horizontal = sortPolygon.firstOrNull{
            it.row == currentRow
        }
        val dot2Horizontal = sortPolygon.lastOrNull{
            dot1Horizontal != null &&
                    it.row == currentRow &&
                    it != dot1Horizontal &&
                    it.column > dot1Horizontal.column
        }?:
        sortPolygon.lastOrNull{
            dot1Horizontal != null &&
                    it.row == currentRow &&
                    it != dot1Horizontal &&
                    it.column < dot1Horizontal.column
        }
        val dot1Vertical = sortPolygon.firstOrNull{
            it.column == currentColumn
        }
        val dot2Vertical = sortPolygon.lastOrNull{
            dot1Vertical != null &&
                    it.column == currentColumn &&
                    it != dot1Vertical &&
                    it.row > dot1Vertical.row
        } ?:
        sortPolygon.lastOrNull{
            dot1Vertical != null &&
                    it.column == currentColumn &&
                    it != dot1Vertical &&
                    it.row < dot1Vertical.row
        }

        return if(dot2Horizontal != null && dot2Vertical!= null){
            ((dot1Horizontal!!.column<currentColumn && currentColumn<dot2Horizontal.column) ||
            (dot1Horizontal.column>currentColumn && currentColumn>dot2Horizontal.column)) &&
            ((dot1Vertical!!.row<currentRow && currentRow<dot2Vertical.row) ||
            (dot1Vertical.row>currentRow && currentRow>dot2Vertical.row))
        } else{
            false
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val field = this.dotGameField ?: return false
        when(event.action){
            //первоначальное событие
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            //пользователь нажал и отпустил палец
            MotionEvent.ACTION_UP -> {
                val row = getRow(event)
                val column = getColumn(event)
                if(row>=0 && column>=0 && row<field.rowsPoints && column<field.columnsPoints){
                    actionListener?.invoke(row,column,field)
                    return true
                }
            }
        }
        return false
    }

    private fun getRow(event: MotionEvent):Int =
        ((event.y-pointRect.top)/cellSize).toInt()

    private fun getColumn(event: MotionEvent):Int =
        ((event.x-pointRect.left)/cellSize).toInt()

    private val listener:OnFieldChangeListener = {
        invalidate()
    }

    companion object{
        private const val DEFAULT_PLAYER_1_COLOR = Color.RED
        private const val DEFAULT_PLAYER_2_COLOR = Color.BLUE
        private const val DEFAULT_GRID_COLOR = Color.GRAY
        private const val DEFAULT_BATTLEFIELD_COLOR = Color.BLACK
        private const val DESIRED_CELL_SIZE = 50f
        private const val RADIUS_POINT = 15f
    }
}