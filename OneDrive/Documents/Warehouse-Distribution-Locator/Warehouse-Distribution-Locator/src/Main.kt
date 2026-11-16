package com.nasugbu.warehouselocator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

// ============================================================================
// 0. HAVERSINE DISTANCE UTILITY & USER AUTH MODEL
// ============================================================================

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.pow(Math.sin(dLat / 2.0), 2.0) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.pow(Math.sin(dLon / 2.0), 2.0)
    val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
    return earthRadiusKm * c
}

data class User(
    val fullName: String,
    val identifier: String,
    val role: String,
    val password: String
)

val NASUGBU_TOWN_PLAZA = GeoPoint(14.0725, 120.6318)

// ============================================================================
// 1. WAREHOUSE ENUMS
// ============================================================================

enum class WarehouseType(val label: String, val colorRgb: Int) {
    CENTRAL("Central Warehouse", 0xFF059669.toInt()),
    FULFILLMENT("Fulfillment Center", 0xFF2563EB.toInt()),
    COLD_STORAGE("Cold Storage", 0xFF0891B2.toInt());

    fun toColor(): Color = Color(colorRgb)
}

enum class WarehouseStatus(val label: String, val colorRgb: Int) {
    ACTIVE("Active", 0xFF10B981.toInt()),
    NEAR_CAPACITY("Near Capacity", 0xFFF59E0B.toInt()),
    MAINTENANCE("Maintenance", 0xFFEF4444.toInt());

    fun toColor(): Color = Color(colorRgb)
}

// ============================================================================
// 2. BASE WAREHOUSE CLASS (ABSTRACT)
// ============================================================================

abstract class BaseWarehouse(
    val id: String,
    val name: String,
    val barangay: String,
    val latitude: Double,
    val longitude: Double,
    val usedPallets: Int,
    val maxPallets: Int,
    val coverageRadiusKm: Double,
    val status: WarehouseStatus,
    val docks: Int,
    val manager: String,
    val hotline: String,
    val hours: String,
    val skus: List<String>
) {
    abstract val type: WarehouseType

    abstract fun getTechnicalSpec(): String

    val utilizationPercent: Int
        get() = if (maxPallets > 0) {
            ((usedPallets.toDouble() / maxPallets) * 100).roundToInt()
        } else 0

    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

    fun getServiceAreaSqKm(): Double {
        return Math.PI * coverageRadiusKm * coverageRadiusKm
    }

    fun distanceFromKm(targetLat: Double, targetLon: Double): Double {
        return calculateDistanceKm(latitude, longitude, targetLat, targetLon)
    }

    fun isWithinCoverage(targetLat: Double, targetLon: Double): Boolean {
        return distanceFromKm(targetLat, targetLon) <= coverageRadiusKm
    }
}

// ============================================================================
// 3. SUBCLASSES DERIVED FROM BASE WAREHOUSE
// ============================================================================

class CentralWarehouse(
    id: String,
    name: String,
    barangay: String,
    latitude: Double,
    longitude: Double,
    usedPallets: Int,
    maxPallets: Int,
    coverageRadiusKm: Double,
    status: WarehouseStatus,
    docks: Int,
    manager: String,
    hotline: String,
    hours: String,
    skus: List<String>,
    val heavyTruckBays: Int
) : BaseWarehouse(
    id, name, barangay, latitude, longitude, usedPallets, maxPallets,
    coverageRadiusKm, status, docks, manager, hotline, hours, skus
) {
    override val type = WarehouseType.CENTRAL

    override fun getTechnicalSpec(): String {
        return "Regional Main Hub - $heavyTruckBays Heavy Truck Bays & Automated Cross-Docking"
    }
}

class FulfillmentCenter(
    id: String,
    name: String,
    barangay: String,
    latitude: Double,
    longitude: Double,
    usedPallets: Int,
    maxPallets: Int,
    coverageRadiusKm: Double,
    status: WarehouseStatus,
    docks: Int,
    manager: String,
    hotline: String,
    hours: String,
    skus: List<String>,
    val dailyParcelCap: Int
) : BaseWarehouse(
    id, name, barangay, latitude, longitude, usedPallets, maxPallets,
    coverageRadiusKm, status, docks, manager, hotline, hours, skus
) {
    override val type = WarehouseType.FULFILLMENT

    override fun getTechnicalSpec(): String {
        return "Express Order Processing - Capacity: $dailyParcelCap Parcels / Day"
    }
}

class ColdStorage(
    id: String,
    name: String,
    barangay: String,
    latitude: Double,
    longitude: Double,
    usedPallets: Int,
    maxPallets: Int,
    coverageRadiusKm: Double,
    status: WarehouseStatus,
    docks: Int,
    manager: String,
    hotline: String,
    hours: String,
    skus: List<String>,
    val temperatureRange: String
) : BaseWarehouse(
    id, name, barangay, latitude, longitude, usedPallets, maxPallets,
    coverageRadiusKm, status, docks, manager, hotline, hours, skus
) {
    override val type = WarehouseType.COLD_STORAGE

    override fun getTechnicalSpec(): String {
        return "Cold Chain Temperature Range: $temperatureRange (Multi-Zone Climate)"
    }
}

// ============================================================================
// 4. NASUGBU WAREHOUSE REPOSITORY DATA
// ============================================================================

val nasugbuWarehouseList: List<BaseWarehouse> = listOf(
    CentralWarehouse(
        id = "NSG-01",
        name = "Nasugbu Central Logistics Hub",
        barangay = "Brgy. Poblacion",
        latitude = 14.0718,
        longitude = 120.6322,
        usedPallets = 380,
        maxPallets = 500,
        coverageRadiusKm = 4.5,
        status = WarehouseStatus.ACTIVE,
        docks = 8,
        manager = "Engr. Mateo Mendoza",
        hotline = "(043) 416-2201",
        hours = "24/7 Operations",
        skus = listOf("General Retail", "FMCG", "Electronics", "Beverages"),
        heavyTruckBays = 6
    ),

    ColdStorage(
        id = "NSG-02",
        name = "Wawa Port Cold Chain Facility",
        barangay = "Brgy. Wawa",
        latitude = 14.0842,
        longitude = 120.6195,
        usedPallets = 220,
        maxPallets = 250,
        coverageRadiusKm = 3.5,
        status = WarehouseStatus.NEAR_CAPACITY,
        docks = 4,
        manager = "Clarissa Dimaano",
        hotline = "(043) 416-3109",
        hours = "24/7 Cold Chain",
        skus = listOf("Seafood", "Frozen Goods", "Dairy", "Meat Products"),
        temperatureRange = "-22°C to +4°C"
    ),

    FulfillmentCenter(
        id = "NSG-03",
        name = "Natipuan Coastal Resort Supply Hub",
        barangay = "Brgy. Natipuan",
        latitude = 14.1325,
        longitude = 120.6148,
        usedPallets = 140,
        maxPallets = 300,
        coverageRadiusKm = 4.0,
        status = WarehouseStatus.ACTIVE,
        docks = 5,
        manager = "Jomar Villalobos",
        hotline = "(043) 416-8840",
        hours = "6:00 AM - 9:00 PM",
        skus = listOf("Hotel Supplies", "Hospitality Amenities", "Resort Goods"),
        dailyParcelCap = 1500
    ),

    CentralWarehouse(
        id = "NSG-04",
        name = "Aga Highland Agro-Distribution Center",
        barangay = "Brgy. Aga",
        latitude = 14.1012,
        longitude = 120.7285,
        usedPallets = 310,
        maxPallets = 400,
        coverageRadiusKm = 5.0,
        status = WarehouseStatus.ACTIVE,
        docks = 6,
        manager = "Ramonito Laurel",
        hotline = "(043) 416-5512",
        hours = "5:00 AM - 8:00 PM",
        skus = listOf("Agri-Produce", "Fertilizers", "Fresh Fruits", "Feeds"),
        heavyTruckBays = 4
    ),

    FulfillmentCenter(
        id = "NSG-05",
        name = "Lumbangan South Border Depot",
        barangay = "Brgy. Lumbangan",
        latitude = 14.0345,
        longitude = 120.6480,
        usedPallets = 195,
        maxPallets = 350,
        coverageRadiusKm = 3.8,
        status = WarehouseStatus.MAINTENANCE,
        docks = 4,
        manager = "Liza Atienza",
        hotline = "(043) 416-7715",
        hours = "7:00 AM - 7:00 PM",
        skus = listOf("Hardware", "Construction Supplies", "Bulk Materials"),
        dailyParcelCap = 1200
    )
)

// ============================================================================
// 5. MAIN ACTIVITY
// ============================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                NasugbuWarehouseApp()
            }
        }
    }
}

// ============================================================================
// 6. MAIN APPLICATION COMPOSABLE
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasugbuWarehouseApp() {
    val context = LocalContext.current

    val registeredUsers = remember { mutableStateListOf<User>() }
    var currentUser by remember { mutableStateOf<User?>(null) }

    var selectedWarehouse by remember { mutableStateOf<BaseWarehouse?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var dropoffPin by remember { mutableStateOf<GeoPoint?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val filteredWarehouses = remember(searchQuery, selectedTypeFilter) {
        nasugbuWarehouseList.filter { warehouse ->
            val matchesType = selectedTypeFilter == "ALL" || warehouse.type.label == selectedTypeFilter
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                    warehouse.name.contains(q, ignoreCase = true) ||
                    warehouse.barangay.contains(q, ignoreCase = true) ||
                    warehouse.id.contains(q, ignoreCase = true) ||
                    warehouse.type.label.contains(q, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

    fun focusOnWarehouse(warehouse: BaseWarehouse?) {
        selectedWarehouse = warehouse
        val map = mapViewRef ?: return
        if (warehouse != null) {
            map.controller.animateTo(warehouse.toGeoPoint())
            map.controller.setZoom(14.5)
        } else {
            val points = nasugbuWarehouseList.map { it.toGeoPoint() }
            if (points.isNotEmpty()) {
                val boundingBox = BoundingBox.fromGeoPointsSafe(ArrayList(points))
                map.zoomToBoundingBox(boundingBox.increaseByScale(1.3f), true)
            }
        }
    }

    DisposableEffect(context) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }

    // Show Auth Screen Overlay first until logged in
    if (currentUser == null) {
        AuthScreenOverlay(
            registeredUsers = registeredUsers,
            onLoginSuccess = { user ->
                currentUser = user
                Toast.makeText(context, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    // Main App Interface
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NASUGBU WAREHOUSE LOCATOR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Batangas Logistics Grid (${filteredWarehouses.size} facilities)",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                },
                actions = {
                    if (dropoffPin != null) {
                        IconButton(onClick = { dropoffPin = null }) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = "Clear Pin",
                                tint = Color(0xFFF59E0B)
                            )
                        }
                    }
                    IconButton(onClick = { focusOnWarehouse(null) }) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Fit All Hubs",
                            tint = Color.White
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = currentUser?.fullName ?: "User",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = currentUser?.role ?: "Coordinator",
                            fontSize = 9.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { currentUser = null }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Log Out",
                            tint = Color(0xFFF87171)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MAP SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                            controller.setCenter(GeoPoint(14.0850, 120.6550))

                            val eventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                    dropoffPin = p
                                    return true
                                }
                                override fun longPressHelper(p: GeoPoint): Boolean = false
                            }
                            overlays.add(0, MapEventsOverlay(eventsReceiver))
                            mapViewRef = this
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()

                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                dropoffPin = p
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint): Boolean = false
                        }
                        mapView.overlays.add(MapEventsOverlay(eventsReceiver))

                        filteredWarehouses.forEach { warehouse ->
                            val isSelected = selectedWarehouse?.id == warehouse.id

                            val circlePoints = Polygon.pointsAsCircle(
                                warehouse.toGeoPoint(),
                                warehouse.coverageRadiusKm * 1000.0
                            )
                            val polygon = Polygon(mapView).apply {
                                points = circlePoints
                                fillPaint.color = android.graphics.Color.argb(
                                    if (isSelected) 70 else 30,
                                    android.graphics.Color.red(warehouse.type.colorRgb),
                                    android.graphics.Color.green(warehouse.type.colorRgb),
                                    android.graphics.Color.blue(warehouse.type.colorRgb)
                                )
                                outlinePaint.color = warehouse.type.colorRgb
                                outlinePaint.strokeWidth = if (isSelected) 5f else 2.5f
                            }
                            mapView.overlays.add(polygon)

                            val marker = Marker(mapView).apply {
                                position = warehouse.toGeoPoint()
                                title = "${warehouse.id}: ${warehouse.name}"
                                snippet = "${warehouse.barangay} • ${warehouse.type.label} (${warehouse.utilizationPercent}% Used)"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { m, _ ->
                                    selectedWarehouse = warehouse
                                    m.showInfoWindow()
                                    mapView.controller.animateTo(warehouse.toGeoPoint())
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                            if (isSelected) {
                                marker.showInfoWindow()
                            }
                        }

                        dropoffPin?.let { pin ->
                            val dropoffMarker = Marker(mapView).apply {
                                position = pin
                                title = "Delivery Drop-off Pin"
                                snippet = "Coords: ${String.format("%.4f, %.4f", pin.latitude, pin.longitude)}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(dropoffMarker)
                            dropoffMarker.showInfoWindow()

                            filteredWarehouses.forEach { warehouse ->
                                val isCovered = warehouse.isWithinCoverage(pin.latitude, pin.longitude)
                                val polyline = Polyline(mapView).apply {
                                    addPoint(warehouse.toGeoPoint())
                                    addPoint(pin)
                                    outlinePaint.color = if (isCovered) warehouse.type.colorRgb else android.graphics.Color.GRAY
                                    outlinePaint.strokeWidth = if (isCovered) 4f else 2f
                                    outlinePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                                }
                                mapView.overlays.add(polyline)
                            }
                        }

                        mapView.invalidate()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "Tap map to set Delivery Drop-off Pin & check coverage",
                        fontSize = 10.sp,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "FACILITY LEGEND",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        WarehouseType.entries.forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(type.toColor(), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = type.label, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // SEARCH & DIRECTORY CONTROLS SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f)
                    .background(Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search warehouse, barangay, or ID...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf(
                            "ALL" to "All (${nasugbuWarehouseList.size})",
                            "Central Warehouse" to "Central",
                            "Fulfillment Center" to "Fulfillment",
                            "Cold Storage" to "Cold Storage"
                        )
                        items(filterOptions) { (typeKey, label) ->
                            val isSelected = selectedTypeFilter == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = typeKey },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }

                // Drop-off Analysis Banner
                dropoffPin?.let { pin ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF27272A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Drop-off Analysis: ${String.format("%.4f, %.4f", pin.latitude, pin.longitude)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                IconButton(
                                    onClick = { dropoffPin = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            val sortedDistances = nasugbuWarehouseList.map { w ->
                                val dist = w.distanceFromKm(pin.latitude, pin.longitude)
                                val covered = w.isWithinCoverage(pin.latitude, pin.longitude)
                                Triple(w, dist, covered)
                            }.sortedBy { it.second }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(sortedDistances) { (wh, dist, covered) ->
                                    Surface(
                                        onClick = { focusOnWarehouse(wh) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (covered) Color(0xFF064E3B) else Color(0xFF3F3F46)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(wh.id, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = wh.type.toColor())
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${String.format("%.2f", dist)} km", fontSize = 10.sp, color = Color.White)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (covered) "[COVERED]" else "[OUT]",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = if (covered) Color(0xFF34D399) else Color(0xFFF87171)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Warehouse Directory List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredWarehouses, key = { it.id }) { warehouse ->
                        WarehouseListItem(
                            warehouse = warehouse,
                            isSelected = selectedWarehouse?.id == warehouse.id,
                            onClick = { selectedWarehouse = warehouse }
                        )
                    }
                }
            }
        }
    }

    // Warehouse Detail Dialog
    selectedWarehouse?.let { warehouse ->
        WarehouseDetailDialog(
            warehouse = warehouse,
            onDismiss = { selectedWarehouse = null },
            onLocateMap = { focusOnWarehouse(warehouse) }
        )
    }
}

// ============================================================================
// 7. AUTHENTICATION OVERLAY SCREEN (LOGIN / SIGN UP)
// ============================================================================

@Composable
fun AuthScreenOverlay(
    registeredUsers: MutableList<User>,
    onLoginSuccess: (User) -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }

    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    var signupName by remember { mutableStateOf("") }
    var signupIdentifier by remember { mutableStateOf("") }
    var signupRole by remember { mutableStateOf("Logistics Coordinator") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    var alertMessage by remember { mutableStateOf<String?>(null) }

    val roleOptions = listOf(
        "Logistics Coordinator",
        "Warehouse Manager",
        "Fleet Dispatcher",
        "Supply Chain Analyst"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF10B981), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PinDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "BATANGAS LOGISTICS GRID",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Nasugbu Warehouse Locator",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in or create a logistics account to access live Nasugbu barangay maps, hub coverage zones, and dispatch controls.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            isLoginTab = true
                            alertMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoginTab) Color(0xFF10B981) else Color(0xFF334155),
                            contentColor = if (isLoginTab) Color.Black else Color.White
                        )
                    ) {
                        Text("Log In", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            isLoginTab = false
                            alertMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isLoginTab) Color(0xFF10B981) else Color(0xFF334155),
                            contentColor = if (!isLoginTab) Color.Black else Color.White
                        )
                    ) {
                        Text("Sign Up", fontWeight = FontWeight.Bold)
                    }
                }

                alertMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7F1D1D)
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (isLoginTab) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = loginIdentifier,
                            onValueChange = { loginIdentifier = it },
                            label = { Text("Email or Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        if (loginPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val id = loginIdentifier.trim().lowercase()
                                val pass = loginPassword
                                val matched = registeredUsers.find { it.identifier == id && it.password == pass }
                                if (matched != null) {
                                    onLoginSuccess(matched)
                                } else {
                                    alertMessage = "Invalid email or password. Please try again or use Quick Demo Login."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Access Warehouse Map", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "— OR INSTANT PREVIEW —",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val demoUser = User(
                                    fullName = "Engr. Mateo Mendoza",
                                    identifier = "demo@nasugbu.ph",
                                    role = "Nasugbu Logistics Coordinator",
                                    password = "demo"
                                )
                                onLoginSuccess(demoUser)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Zap, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Demo Login (Coordinator)", color = Color.White, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { signupName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = signupIdentifier,
                            onValueChange = { signupIdentifier = it },
                            label = { Text("Email or Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Logistics Role:", fontSize = 11.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(roleOptions) { role ->
                                FilterChip(
                                    selected = signupRole == role,
                                    onClick = { signupRole = role },
                                    label = { Text(role, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF10B981),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = signupPassword,
                            onValueChange = { signupPassword = it },
                            label = { Text("Create Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                    Icon(
                                        if (signupPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val name = signupName.trim()
                                val id = signupIdentifier.trim().lowercase()
                                val pass = signupPassword

                                if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
                                    alertMessage = "Please complete all required fields."
                                    return@Button
                                }
                                if (registeredUsers.any { it.identifier == id }) {
                                    alertMessage = "An account with this email/username already exists. Please log in."
                                    return@Button
                                }

                                val newUser = User(fullName = name, identifier = id, role = signupRole, password = pass)
                                registeredUsers.add(newUser)
                                onLoginSuccess(newUser)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Account & Open Map", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. WAREHOUSE LIST ITEM COMPOSABLE
// ============================================================================

@Composable
fun WarehouseListItem(
    warehouse: BaseWarehouse,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColor = warehouse.type.toColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.6f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (warehouse.type) {
                            WarehouseType.CENTRAL -> Icons.Default.Store
                            WarehouseType.FULFILLMENT -> Icons.Default.LocalShipping
                            WarehouseType.COLD_STORAGE -> Icons.Default.AcUnit
                        },
                        contentDescription = warehouse.type.label,
                        tint = typeColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = warehouse.id,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = warehouse.barangay,
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                    Text(
                        text = warehouse.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${warehouse.type.label} • ${warehouse.coverageRadiusKm} km radius",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = warehouse.status.toColor().copy(alpha = 0.2f)
                ) {
                    Text(
                        text = warehouse.status.label,
                        color = warehouse.status.toColor(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Capacity: ${warehouse.usedPallets} / ${warehouse.maxPallets} Pallets",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "${warehouse.utilizationPercent}% Utilized",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (warehouse.utilizationPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = typeColor,
                trackColor = Color.DarkGray
            )
        }
    }
}

// ============================================================================
// 9. WAREHOUSE DETAIL DIALOG COMPOSABLE
// ============================================================================

@Composable
fun WarehouseDetailDialog(
    warehouse: BaseWarehouse,
    onDismiss: () -> Unit,
    onLocateMap: () -> Unit
) {
    val typeColor = warehouse.type.toColor()
    val townPlazaDist = warehouse.distanceFromKm(NASUGBU_TOWN_PLAZA.latitude, NASUGBU_TOWN_PLAZA.longitude)
    val isPlazaCovered = warehouse.isWithinCoverage(NASUGBU_TOWN_PLAZA.latitude, NASUGBU_TOWN_PLAZA.longitude)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = warehouse.id,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = typeColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = warehouse.barangay,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = warehouse.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = warehouse.getTechnicalSpec(),
                        fontSize = 11.sp,
                        color = typeColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("• Status: ${warehouse.status.label}", color = warehouse.status.toColor(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("• Pallet Capacity: ${warehouse.usedPallets} / ${warehouse.maxPallets} (${warehouse.utilizationPercent}%)", color = Color.White, fontSize = 12.sp)
                Text("• Coverage Radius: ${warehouse.coverageRadiusKm} km", color = Color.White, fontSize = 12.sp)
                Text("• Service Area: ${String.format("%.2f", warehouse.getServiceAreaSqKm())} sq km", color = Color.White, fontSize = 12.sp)
                Text("• Distance to Town Plaza: ${String.format("%.2f", townPlazaDist)} km (${if (isPlazaCovered) "Covered" else "Outside Radius"})", color = if (isPlazaCovered) Color(0xFF34D399) else Color(0xFFF87171), fontSize = 12.sp)
                Text("• Loading Docks: ${warehouse.docks}", color = Color.White, fontSize = 12.sp)
                Text("• Facility Manager: ${warehouse.manager}", color = Color.White, fontSize = 12.sp)
                Text("• Direct Hotline: ${warehouse.hotline}", color = Color.White, fontSize = 12.sp)
                Text("• Operating Hours: ${warehouse.hours}", color = Color.White, fontSize = 12.sp)
                Text("• Handled SKUs: ${warehouse.skus.joinToString()}", color = Color.LightGray, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLocateMap()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Focus on Map", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B),
        shape = RoundedCornerShape(14.dp)
    )
}