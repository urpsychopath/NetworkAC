package ci.miage.mob.networkAC
import androidx.core.view.GravityCompat
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import ci.miage.mob.networkAC.R
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var graphView: GraphView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        graphView = findViewById(R.id.graphView)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_add_node -> {
                val nodeLabel = getString(R.string.node_label_format, graphView.nodeCount + 1)
                graphView.addNode(nodeLabel)
                Toast.makeText(this, getString(R.string.toast_node_added), Toast.LENGTH_SHORT).show()
            }
            R.id.nav_add_connection -> {
                graphView.currentMode = GraphView.Mode.ADD_CONNECTION
                Toast.makeText(this, getString(R.string.toast_mode_connection), Toast.LENGTH_SHORT).show()
            }
            R.id.nav_reset_graph -> {
                graphView.resetGraph()

                Toast.makeText(this, getString(R.string.toast_reset_connections), Toast.LENGTH_SHORT).show()
            }
            R.id.nav_move_node -> {
                graphView.currentMode = GraphView.Mode.MOVE_NODE
                Toast.makeText(this, getString(R.string.toast_mode_move), Toast.LENGTH_SHORT).show()
            }
            R.id.nav_save_graph -> {
                graphView.saveGraph()
            }
            R.id.nav_load_graph -> {
                graphView.loadGraph()
            }
        }
        drawerLayout.closeDrawers()
        return true
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
