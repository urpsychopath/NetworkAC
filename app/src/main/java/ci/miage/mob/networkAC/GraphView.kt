package ci.miage.mob.networkAC

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.text.InputType
import android.util.AttributeSet
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GestureDetectorCompat
import org.json.JSONObject
import kotlin.math.hypot
import kotlin.random.Random
import android.app.Activity

import ci.miage.mob.networkAC.R
/**
 * Vue customisée qui s'occupe de l'affichage du graphe et des interactions.
 * Elle utilise une instance du modèle Graph pour gérer les données.
 */
class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Mode { ADD_NODE, ADD_CONNECTION, MOVE_NODE, NONE }
    var currentMode: Mode = Mode.ADD_NODE

    // Instance du modèle
    val graph = Graph()
    val nodeCount: Int get() = graph.nodes.size

    // Variables d'interaction
    private var connectionStartNode: Graph.Node? = null
    private var tempEndX: Float = 0f
    private var tempEndY: Float = 0f
    private var movingNode: Graph.Node? = null
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    // Paints
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

    // Fond (ex : plan d'appartement)
    private var backgroundBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.appartement_plan)

    // Détecteur de gestes pour le long‑click
    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            // Vérifier s'il existe un nœud proche
            findNearestNode(e.x, e.y, 50f)?.let { node ->
                // Si le nœud a pour label "Déplacer l'objet", vérifier si le toucher est sur le texte
                if (node.label == context.getString(R.string.move_object)) {
                    val textWidth = textPaint.measureText(node.label)
                    val textHeight = textPaint.textSize
                    val textRect = RectF(
                        node.x - 20,
                        node.y - 40 - textHeight,
                        node.x - 20 + textWidth,
                        node.y - 40
                    )
                    if (textRect.contains(e.x, e.y)) {
                        currentMode = Mode.MOVE_NODE
                        Toast.makeText(context, context.getString(R.string.toast_mode_move), Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                // Sinon afficher le menu contextuel du nœud
                showNodeContextMenu(node, e.x, e.y)
                return
            }
            // Vérifier s'il existe une connexion proche
            findNearestConnection(e.x, e.y, 50f)?.let { connection ->
                showConnectionContextMenu(connection, e.x, e.y)
                return
            }
            // Aucun objet détecté : afficher une boîte de dialogue pour ajouter un nœud
            showAddNodeDialog(e.x, e.y)
        }
    })
    fun resetGraph() {
        graph.nodes.clear()
        graph.connections.clear()
        invalidate()
    }

    // Ajoute un nœud avec des coordonnées aléatoires
    fun addNode(name: String) {
        // Position aléatoire mais limitée à l'intérieur du plan
        val radius = 30f
        val maxX = backgroundBitmap?.width?.toFloat() ?: width.toFloat()
        val maxY = backgroundBitmap?.height?.toFloat() ?: height.toFloat()
        val x = (Random.nextFloat() * (maxX - 2 * radius)) + radius
        val y = (Random.nextFloat() * (maxY - 2 * radius)) + radius
        graph.addNode(Graph.Node(x, y, Color.BLUE, name))
        invalidate()
    }

    // Ajoute un nœud à une position précise en le limitant au plan
    fun createNodeAt(x: Float, y: Float, label: String) {
        val radius = 30f
        val maxX = backgroundBitmap?.width?.toFloat() ?: width.toFloat()
        val maxY = backgroundBitmap?.height?.toFloat() ?: height.toFloat()
        val clampedX = x.coerceIn(radius, maxX - radius)
        val clampedY = y.coerceIn(radius, maxY - radius)
        graph.addNode(Graph.Node(clampedX, clampedY, Color.BLUE, label))
        invalidate()
    }

    // Affiche une boîte de dialogue pour saisir l'étiquette d'un nouveau nœud
    private fun showAddNodeDialog(x: Float, y: Float) {
        val editText = EditText(context).apply { hint = context.getString(R.string.enter_label) }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_object))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.button_add)) { dialog, _ ->
                val label = editText.text.toString().ifEmpty { context.getString(R.string.node_default_label) }
                createNodeAt(x, y, label)
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    // Sauvegarde du graphe dans la mémoire interne
    fun saveGraph() {
        try {
            val json = graph.toJson()
            context.openFileOutput("graph.json", Context.MODE_PRIVATE).use { fos ->
                fos.write(json.toString().toByteArray())
            }
            Toast.makeText(context, context.getString(R.string.save_graph_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.save_graph_error), Toast.LENGTH_SHORT).show()
        }
    }

    // Chargement du graphe depuis la mémoire interne
    fun loadGraph() {
        try {
            context.openFileInput("graph.json").use { fis ->
                val jsonStr = fis.bufferedReader().readText()
                val json = JSONObject(jsonStr)
                graph.loadFromJson(json)
                // Recharger les icônes pour chaque nœud qui a un iconResId valide
                for (node in graph.nodes) {
                    node.iconResId?.let { resId ->
                        if (resId != -1) {
                            node.icon = BitmapFactory.decodeResource(context.resources, resId)
                        }
                    }
                }
                invalidate()
            }
            Toast.makeText(context, context.getString(R.string.load_graph_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.load_graph_error), Toast.LENGTH_SHORT).show()
        }
    }

    // Retourne un DrawableGraph généré à partir du modèle
    fun getGraphDrawable(): DrawableGraph {
        return DrawableGraph(graph.nodes, graph.connections, backgroundBitmap)
    }

    // Réinitialise uniquement les connexions du graphe
    fun resetConnections() {
        graph.resetConnections()
        invalidate()
    }

    // Dessin de la vue
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) } ?: canvas.drawColor(Color.LTGRAY)

        // Dessiner les connexions
        for (conn in graph.connections) {
            connectionPaint.color = conn.color
            connectionPaint.strokeWidth = conn.thickness
            canvas.drawLine(conn.start.x, conn.start.y, conn.end.x, conn.end.y, connectionPaint)
            val midX = (conn.start.x + conn.end.x) / 2
            val midY = (conn.start.y + conn.end.y) / 2
            canvas.drawText(conn.label, midX, midY, textPaint)
        }

        // Afficher la ligne temporaire lors de la création d'une connexion
        if (currentMode == Mode.ADD_CONNECTION && connectionStartNode != null) {
            canvas.drawLine(connectionStartNode!!.x, connectionStartNode!!.y, tempEndX, tempEndY, connectionPaint)
        }

        // Dessiner les nœuds
        for (node in graph.nodes) {
            if (node.icon != null) {
                // Définissez la taille souhaitée en dp (ici 60dp)
                val desiredDp = 60
                val density = resources.displayMetrics.density
                val desiredSize = (desiredDp * density).toInt() // conversion en pixels
                // Redimensionnez l'icône
                val scaledIcon = Bitmap.createScaledBitmap(node.icon!!, desiredSize, desiredSize, true)
                // Dessinez l'icône centrée sur les coordonnées du nœud
                canvas.drawBitmap(scaledIcon, node.x - desiredSize / 2, node.y - desiredSize / 2, null)
                // Dessinez le label sous l'icône : centré horizontalement et avec un petit décalage vertical
                val labelWidth = textPaint.measureText(node.label)
                canvas.drawText(node.label, node.x - labelWidth / 2, node.y + desiredSize / 2 + textPaint.textSize, textPaint)
            } else {
                // Pas d'icône : dessinez le cercle et le label aux positions d'origine
                nodePaint.color = node.color
                canvas.drawCircle(node.x, node.y, 30f, nodePaint)
                canvas.drawText(node.label, node.x - 20, node.y - 40, textPaint)
            }
        }



    }


    private fun showAddConnectionDialog(startNode: Graph.Node, endNode: Graph.Node) {
        val editText = EditText(context).apply {
            hint = context.getString(R.string.enter_label)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_connection)) // ou un titre explicite comme "Ajouter une connexion"
            .setView(editText)
            .setPositiveButton(context.getString(R.string.button_add)) { dialog, _ ->
                val label = editText.text.toString().ifEmpty { context.getString(R.string.connection_default_label) }
                graph.addConnection(Graph.Connection(startNode, endNode, label))
                invalidate()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return when (currentMode) {
            Mode.ADD_CONNECTION -> handleAddConnection(event)
            Mode.MOVE_NODE -> handleMoveNode(event)
            else -> super.onTouchEvent(event)
        }
    }

    private fun handleAddConnection(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                findNearestNode(event.x, event.y, 50f)?.let {
                    connectionStartNode = it
                    tempEndX = event.x
                    tempEndY = event.y
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                connectionStartNode?.let {
                    tempEndX = event.x
                    tempEndY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                connectionStartNode?.let { startNode ->
                    findNearestNode(event.x, event.y, 100f)?.let { endNode ->
                        if (endNode != startNode && !graph.connectionExists(startNode, endNode)) {
                            // Au lieu de créer automatiquement, afficher un dialogue pour saisir l’étiquette
                            showAddConnectionDialog(startNode, endNode)
                        }
                    }
                    connectionStartNode = null
                    invalidate()
                    return true
                }
            }
        }
        return false
    }

    private fun handleMoveNode(event: MotionEvent): Boolean {
        // Empêcher le parent d'intercepter pour éviter le déplacement du plan
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                findNearestNode(event.x, event.y, 50f)?.let {
                    movingNode = it
                    offsetX = event.x - it.x
                    offsetY = event.y - it.y
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                movingNode?.let {
                    val radius = 30f
                    val maxX = backgroundBitmap?.width?.toFloat() ?: width.toFloat()
                    val maxY = backgroundBitmap?.height?.toFloat() ?: height.toFloat()
                    val newX = (event.x - offsetX).coerceIn(radius, maxX - radius)
                    val newY = (event.y - offsetY).coerceIn(radius, maxY - radius)
                    it.x = newX
                    it.y = newY
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> movingNode = null
        }
        return false
    }

    private fun findNearestNode(x: Float, y: Float, baseRadius: Float): Graph.Node? {
        var closest: Graph.Node? = null
        var minDist = Float.MAX_VALUE
        val density = resources.displayMetrics.density
        val desiredDp = 60
        val desiredSize = (desiredDp * density).toInt() // taille en pixels, identique à celle utilisée en onDraw

        for (node in graph.nodes) {
            // Si le nœud a une icône, on utilise la moitié de la taille redimensionnée comme rayon de détection,
            // sinon on utilise le rayon par défaut (ex: 50f ou baseRadius).
            val detectionRadius = if (node.icon != null) desiredSize / 2f else baseRadius
            val d = hypot(x - node.x, y - node.y)
            if (d <= detectionRadius && d < minDist) {
                minDist = d
                closest = node
            }
        }
        return closest
    }


    private fun findNearestConnection(x: Float, y: Float, threshold: Float): Graph.Connection? {
        var closest: Graph.Connection? = null
        var minDist = Float.MAX_VALUE
        for (conn in graph.connections) {
            val d = distancePointToSegment(x, y, conn.start.x, conn.start.y, conn.end.x, conn.end.y)
            if (d < threshold && d < minDist) {
                minDist = d
                closest = conn
            }
        }
        return closest
    }

    // --------------------------
    // Menus contextuels
    // --------------------------
    private fun showNodeContextMenu(node: Graph.Node, x: Float, y: Float) {
        // Récupérer la vue racine de l'activité
        val rootView = (context as? Activity)?.findViewById<ViewGroup>(android.R.id.content)
        if (rootView == null) {
            // Si non trouvée, utiliser le fallback avec l'ancre sur "this"
            val fallbackPopup = PopupMenu(context, this)
            fallbackPopup.menuInflater.inflate(R.menu.node_context, fallbackPopup.menu)
            fallbackPopup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete_node -> {
                        graph.removeNode(node)
                        invalidate()
                        true
                    }
                    R.id.menu_change_color -> {
                        showNodeColorPickerDialog(node)
                        true
                    }
                    R.id.menu_edit_label -> {
                        showEditLabelDialog(node)
                        true
                    }
                    R.id.menu_move_node -> {
                        currentMode = Mode.MOVE_NODE
                        Toast.makeText(context, context.getString(R.string.toast_mode_move), Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.menu_choose_icon -> {
                        showIconChooserDialog(node)
                        true
                    }
                    else -> false
                }
            }
            fallbackPopup.show()
            return
        }

        // Créer une vue ancre minimale (1x1 pixel)
        val anchorView = View(context)
        val layoutParams = ViewGroup.LayoutParams(1, 1)
        anchorView.layoutParams = layoutParams
        rootView.addView(anchorView)

        // Calculer la position absolue de GraphView sur l'écran
        val viewLocation = IntArray(2)
        this.getLocationOnScreen(viewLocation)

        // Calculer la position absolue de la vue racine
        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)

        // Positionner l'ancre dans la vue racine en ajustant la position
        anchorView.x = viewLocation[0] + x - rootLocation[0]
        anchorView.y = viewLocation[1] + y - rootLocation[1]

        // Créer le PopupMenu en utilisant l'ancre et NO_GRAVITY
        val popup = PopupMenu(context, anchorView, Gravity.NO_GRAVITY)
        popup.menuInflater.inflate(R.menu.node_context, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete_node -> {
                    graph.removeNode(node)
                    invalidate()
                    true
                }
                R.id.menu_change_color -> {
                    showNodeColorPickerDialog(node)
                    true
                }
                R.id.menu_edit_label -> {
                    showEditLabelDialog(node)
                    true
                }
                R.id.menu_move_node -> {
                    currentMode = Mode.MOVE_NODE
                    Toast.makeText(context, context.getString(R.string.toast_mode_move), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_choose_icon -> {
                    showIconChooserDialog(node)
                    true
                }
                else -> false
            }
        }
        // Supprimer l'ancre une fois le menu fermé
        popup.setOnDismissListener {
            rootView.removeView(anchorView)
        }
        popup.show()
    }

    private fun showConnectionContextMenu(conn: Graph.Connection, x: Float, y: Float) {
        val rootView = (context as? Activity)?.findViewById<ViewGroup>(android.R.id.content)
        if (rootView == null) {
            val fallbackPopup = PopupMenu(context, this)
            fallbackPopup.menuInflater.inflate(R.menu.connection_context, fallbackPopup.menu)
            fallbackPopup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete_connection -> {
                        graph.connections.remove(conn)
                        invalidate()
                        true
                    }
                    R.id.menu_edit_connection_label -> {
                        showConnectionLabelDialog(conn)
                        true
                    }
                    R.id.menu_change_connection_color -> {
                        showConnectionColorPickerDialog(conn)
                        true
                    }
                    R.id.menu_change_connection_thickness -> {
                        showConnectionThicknessDialog(conn)
                        true
                    }
                    else -> false
                }
            }
            fallbackPopup.show()
            return
        }

        val anchorView = View(context)
        val layoutParams = ViewGroup.LayoutParams(1, 1)
        anchorView.layoutParams = layoutParams
        rootView.addView(anchorView)

        val viewLocation = IntArray(2)
        this.getLocationOnScreen(viewLocation)
        val rootLocation = IntArray(2)
        rootView.getLocationOnScreen(rootLocation)

        anchorView.x = viewLocation[0] + x - rootLocation[0]
        anchorView.y = viewLocation[1] + y - rootLocation[1]

        val popup = PopupMenu(context, anchorView, Gravity.NO_GRAVITY)
        popup.menuInflater.inflate(R.menu.connection_context, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete_connection -> {
                    graph.connections.remove(conn)
                    invalidate()
                    true
                }
                R.id.menu_edit_connection_label -> {
                    showConnectionLabelDialog(conn)
                    true
                }
                R.id.menu_change_connection_color -> {
                    showConnectionColorPickerDialog(conn)
                    true
                }
                R.id.menu_change_connection_thickness -> {
                    showConnectionThicknessDialog(conn)
                    true
                }
                else -> false
            }
        }
        popup.setOnDismissListener {
            rootView.removeView(anchorView)
        }
        popup.show()
    }

    private fun showIconChooserDialog(node: Graph.Node) {

        val iconNames = arrayOf("Imprimante", "Télévision", "Lampe", "Caméra")
        val iconResIds = arrayOf(
            R.drawable.icon_printer,
            R.drawable.icon_tv,
            R.drawable.icon_lamp,
            R.drawable.icon_camera
        )

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.choose_icon))
            .setItems(iconNames) { dialog, which ->
                val chosenResId = iconResIds[which]
                // Stocker l'ID et le bitmap dans le nœud
                node.iconResId = chosenResId
                node.icon = BitmapFactory.decodeResource(context.resources, chosenResId)
                invalidate()
                Toast.makeText(context, context.getString(R.string.icon_selected), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }


    private fun showNodeColorPickerDialog(node: Graph.Node) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_node_color_picker, null)
        var selectedColor = node.color
        dialogView.findViewById<View>(R.id.color_red).setOnClickListener {
            selectedColor = Color.RED
            Toast.makeText(context, context.getString(R.string.toast_color_red), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_green).setOnClickListener {
            selectedColor = Color.GREEN
            Toast.makeText(context, context.getString(R.string.toast_color_green), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_blue).setOnClickListener {
            selectedColor = Color.BLUE
            Toast.makeText(context, context.getString(R.string.toast_color_blue), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_orange).setOnClickListener {
            selectedColor = Color.parseColor("#FFA500")
            Toast.makeText(context, context.getString(R.string.toast_color_orange), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_cyan).setOnClickListener {
            selectedColor = Color.CYAN
            Toast.makeText(context, context.getString(R.string.toast_color_cyan), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_magenta).setOnClickListener {
            selectedColor = Color.MAGENTA
            Toast.makeText(context, context.getString(R.string.toast_color_magenta), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_black).setOnClickListener {
            selectedColor = Color.BLACK
            Toast.makeText(context, context.getString(R.string.toast_color_black), Toast.LENGTH_SHORT).show()
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.change_color))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.button_ok)) { dialog, _ ->
                node.color = selectedColor
                // Réinitialiser l'icône pour revenir au mode "nœud" classique
                node.icon = null
                node.iconResId = -1  // ou null selon la déclaration de la variable
                invalidate()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showEditLabelDialog(node: Graph.Node) {
        val editText = EditText(context).apply {
            setText(node.label)
            hint = context.getString(R.string.edit_label)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.button_ok)) { _, _ ->
                node.label = editText.text.toString()
                invalidate()
            }
            .setNegativeButton(context.getString(R.string.button_cancel), null)
            .show()
    }

    private fun showConnectionLabelDialog(conn: Graph.Connection) {
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(conn.label)
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_connection_label))
            .setView(editText)
            .setPositiveButton(context.getString(R.string.button_ok)) { dialog, _ ->
                conn.label = editText.text.toString()
                invalidate()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun distancePointToSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) {
            return hypot(px - x1, py - y1)
        }
        val t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)
        val clampedT = t.coerceIn(0f, 1f)
        val projX = x1 + clampedT * dx
        val projY = y1 + clampedT * dy
        return hypot(px - projX, py - projY)
    }

    private fun showConnectionColorPickerDialog(conn: Graph.Connection) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_connection_color_picker, null)
        var selectedColor = conn.color
        dialogView.findViewById<View>(R.id.color_red).setOnClickListener {
            selectedColor = Color.RED
            Toast.makeText(context, context.getString(R.string.toast_color_red), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_green).setOnClickListener {
            selectedColor = Color.GREEN
            Toast.makeText(context, context.getString(R.string.toast_color_green), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_blue).setOnClickListener {
            selectedColor = Color.BLUE
            Toast.makeText(context, context.getString(R.string.toast_color_blue), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_orange).setOnClickListener {
            selectedColor = Color.parseColor("#FFA500")
            Toast.makeText(context, context.getString(R.string.toast_color_orange), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_cyan).setOnClickListener {
            selectedColor = Color.CYAN
            Toast.makeText(context, context.getString(R.string.toast_color_cyan), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_magenta).setOnClickListener {
            selectedColor = Color.MAGENTA
            Toast.makeText(context, context.getString(R.string.toast_color_magenta), Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<View>(R.id.color_black).setOnClickListener {
            selectedColor = Color.BLACK
            Toast.makeText(context, context.getString(R.string.toast_color_black), Toast.LENGTH_SHORT).show()
        }
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.change_connection_color))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.button_ok)) { dialog, _ ->
                conn.color = selectedColor
                invalidate()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showConnectionThicknessDialog(conn: Graph.Connection) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_connection_thickness, null)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarThickness)
        val textView = dialogView.findViewById<TextView>(R.id.textThicknessValue)
        seekBar.progress = conn.thickness.toInt()
        textView.text = context.getString(R.string.thickness_value, conn.thickness.toInt())
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = context.getString(R.string.thickness_value, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.change_connection_thickness))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.button_ok)) { dialog, _ ->
                conn.thickness = seekBar.progress.toFloat().coerceAtLeast(1f)
                invalidate()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.button_cancel)) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
