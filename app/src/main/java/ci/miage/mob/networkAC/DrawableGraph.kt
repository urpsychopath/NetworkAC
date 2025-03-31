package ci.miage.mob.networkAC

import android.graphics.*
import android.graphics.drawable.Drawable

class DrawableGraph(
    private val nodes: List<Graph.Node>,
    private val connections: List<Graph.Connection>,
    private val backgroundBitmap: Bitmap? = null
) : Drawable() {

    private val nodePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
    }
    private val connectionPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        } ?: canvas.drawColor(Color.LTGRAY)

        for (conn in connections) {
            connectionPaint.color = conn.color
            connectionPaint.strokeWidth = conn.thickness
            if (conn.curvature == 0f) {
                canvas.drawLine(conn.start.x, conn.start.y, conn.end.x, conn.end.y, connectionPaint)
                val midX = (conn.start.x + conn.end.x) / 2
                val midY = (conn.start.y + conn.end.y) / 2
                canvas.drawText(conn.label, midX, midY, textPaint)
            } else {
                val path = Path()
                path.moveTo(conn.start.x, conn.start.y)
                val midX = (conn.start.x + conn.end.x) / 2
                val midY = (conn.start.y + conn.end.y) / 2
                val dx = conn.end.x - conn.start.x
                val dy = conn.end.y - conn.start.y
                val length = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val px = if (length != 0f) -dy / length else 0f
                val py = if (length != 0f) dx / length else 0f
                val controlX = midX + px * conn.curvature
                val controlY = midY + py * conn.curvature
                path.quadTo(controlX, controlY, conn.end.x, conn.end.y)
                canvas.drawPath(path, connectionPaint)
                val labelX = 0.25f * conn.start.x + 0.5f * controlX + 0.25f * conn.end.x
                val labelY = 0.25f * conn.start.y + 0.5f * controlY + 0.25f * conn.end.y
                canvas.drawText(conn.label, labelX, labelY, textPaint)
            }
        }
        for (node in nodes) {
            nodePaint.color = node.color
            canvas.drawCircle(node.x, node.y, 30f, nodePaint)
            canvas.drawText(node.label, node.x - 20, node.y - 40, textPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        nodePaint.alpha = alpha
        textPaint.alpha = alpha
        connectionPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        nodePaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        connectionPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
