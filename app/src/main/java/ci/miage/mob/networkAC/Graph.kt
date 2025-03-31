package ci.miage.mob.networkAC
import android.graphics.Bitmap
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

class Graph {
    val nodes = mutableListOf<Node>()
    val connections = mutableListOf<Connection>()

    fun addNode(node: Node) {
        nodes.add(node)
    }

    fun removeNode(node: Node) {
        nodes.remove(node)
        // Supprime toutes les connexions associées au nœud
        connections.removeAll { it.start == node || it.end == node }
    }

    fun addConnection(connection: Connection) {
        if (!connectionExists(connection.start, connection.end)) {
            connections.add(connection)
        }
    }

    fun connectionExists(a: Node, b: Node): Boolean {
        return connections.any { (it.start == a && it.end == b) || (it.start == b && it.end == a) }
    }

    fun resetConnections() {
        connections.clear()
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        val nodesArray = JSONArray()
        nodes.forEach { node ->
            val nodeJson = JSONObject().apply {
                put("x", node.x)
                put("y", node.y)
                put("color", node.color)
                put("label", node.label)
                // Sauvegarder l'ID de la ressource, ou -1 s'il n'y a pas d'icône
                put("iconResId", node.iconResId ?: -1)
            }
            nodesArray.put(nodeJson)
        }
        json.put("nodes", nodesArray)

        // Sauvegarde des connexions (inchangé)
        val connectionsArray = JSONArray()
        connections.forEach { conn ->
            val startIndex = nodes.indexOf(conn.start)
            val endIndex = nodes.indexOf(conn.end)
            val connJson = JSONObject().apply {
                put("startIndex", startIndex)
                put("endIndex", endIndex)
                put("label", conn.label)
                put("color", conn.color)
                put("thickness", conn.thickness)
                put("curvature", conn.curvature)
            }
            connectionsArray.put(connJson)
        }
        json.put("connections", connectionsArray)
        return json
    }

    fun loadFromJson(json: JSONObject) {
        nodes.clear()
        connections.clear()
        val nodesArray = json.getJSONArray("nodes")
        for (i in 0 until nodesArray.length()) {
            val nodeJson = nodesArray.getJSONObject(i)
            val node = Node(
                nodeJson.getDouble("x").toFloat(),
                nodeJson.getDouble("y").toFloat(),
                nodeJson.getInt("color"),
                nodeJson.getString("label")
            )
            // Lire l'ID de l'icône
            val iconResId = nodeJson.optInt("iconResId", -1)
            if (iconResId != -1) {
                node.iconResId = iconResId
                // Ne pas créer le Bitmap ici, vous le ferez dans GraphView
            }
            nodes.add(node)
        }
        // Recharger les connexions comme avant
        val connectionsArray = json.getJSONArray("connections")
        for (i in 0 until connectionsArray.length()) {
            val connJson = connectionsArray.getJSONObject(i)
            val startIndex = connJson.getInt("startIndex")
            val endIndex = connJson.getInt("endIndex")
            if (startIndex in nodes.indices && endIndex in nodes.indices) {
                val connection = Graph.Connection(
                    nodes[startIndex],
                    nodes[endIndex],
                    connJson.getString("label"),
                    connJson.getInt("color"),
                    connJson.getDouble("thickness").toFloat(),
                    connJson.optDouble("curvature", 0.0).toFloat()
                )
                connections.add(connection)
            }
        }
    }

    data class Node(
        var x: Float,
        var y: Float,
        var color: Int = Color.BLUE,
        var label: String = "Nœud",
        var icon: Bitmap? = null,
        var iconResId: Int? = null // Ajout de la référence à la ressource
    )

    class Connection(
        val start: Node,
        val end: Node,
        var label: String,
        var color: Int = Color.BLACK,
        var thickness: Float = 5f,
        var curvature: Float = 0f // 0 = ligne droite ; sinon, décalage perpendiculaire en pixels
    )
}
