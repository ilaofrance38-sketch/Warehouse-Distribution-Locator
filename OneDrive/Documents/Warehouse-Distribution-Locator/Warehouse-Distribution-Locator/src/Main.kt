package com.example.nasugbuwarehouselocator

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// ==========================================
// 1. BLUE COLOR PALETTE (THEME)
// ==========================================
val DeepBlue = Color(0xFF0D47A1)
val PrimaryBlue = Color(0xFF1565C0)
val BrightBlue = Color(0xFF1E88E5)
val LightBlueBg = Color(0xFFE3F2FD)
val SurfaceWhite = Color(0xFFFFFFFF)

private val AppBlueColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlueBg,
    onPrimaryContainer = DeepBlue,
    secondary = BrightBlue,
    background = LightBlueBg,
    surface = SurfaceWhite
)

// ==========================================
// 2. DATA MODELS & NASUGBU WAREHOUSE DATA
// ==========================================
data class UserAccount(
    val fullName: String,
    val email: String,
    val password: String
)

data class Warehouse(
    val id: String,
    val name: String,
    val barangay: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: String,
    val status: String,
    val contact: String,
    val goodsType: String
)

// Listahan ng mga Warehouse Distribution Hubs sa Nasugbu, Batangas
val nasugbuWarehouses = listOf(
    Warehouse(
        id = "WH-01",
        name = "Nasugbu Central Warehouse",
        barangay = "Brgy. Poblacion, Nasugbu",
        latitude = 14.0695,
        longitude = 120.6322,
        capacity = "85% Full (12,000 pallets)",
        status = "Operational",
        contact = "Logistics Coordinator: 0917-555-0101",
        goodsType = "FMCG & Retail Goods"
    ),
    Warehouse(
        id = "WH-02",
        name = "Wawa Cold Storage Hub",
        barangay = "Brgy. Wawa, Nasugbu",
        latitude = 14.0842,
        longitude = 120.6208,
        capacity = "60% Full (8,500 pallets)",
        status = "Operational",
        contact = "Warehouse Manager: 0917-555-0102",
        goodsType = "Cold Storage & Seafood"
    ),
    Warehouse(
        id = "WH-03",
        name = "Lumbangan Fulfillment Center",
        barangay = "Brgy. Lumbangan, Nasugbu",
        latitude = 14.0588,
        longitude = 120.6725,
        capacity = "72% Full (15,000 pallets)",
        status = "Operational",
        contact = "Logistics Coordinator: 0917-555-0103",
        goodsType = "Sugar, Rice & Agricultural Supplies"
    ),
    Warehouse(
        id = "WH-04",
        name = "Aga Highway Transit Warehouse",
        barangay = "Brgy. Aga, Nasugbu",
        latitude = 14.0890,
        longitude = 120.7350,
        capacity = "45% Full (10,000 pallets)",
        status = "Operational",
        contact = "Warehouse Manager: 0917-555-0104",
        goodsType = "Construction & Hardware"
    ),
    Warehouse(
        id = "WH-05",
        name = "Caylaway Highland Cold Chain",
        barangay = "Brgy. Caylaway, Nasugbu",
        latitude = 14.0825,
        longitude = 120.7930,
        capacity = "90% Full (6,000 pallets)",
        status = "High Volume",
        contact = "Logistics Coordinator: 0917-555-0105",
        goodsType = "Fresh Produce & Dairy"
    ),
    Warehouse(
        id = "WH-06",
        name = "Natipuan Coastal Supply Depot",
        barangay = "Brgy. Natipuan, Nasugbu",
        latitude = 14.1295,
        longitude = 120.6255,
        capacity = "30% Full (5,000 pallets)",
        status = "Operational",
        contact = "Warehouse Manager: 0917-555-0106",
        goodsType = "Hospitality & Resort Supplies"
    ),
    Warehouse(
        id = "WH-07",
        name = "Bucana South Logistics Center",
        barangay = "Brgy. Bucana, Nasugbu",
        latitude = 14.0520,
        longitude = 120.6285,
        capacity = "55% Full (7,200 pallets)",
        status = "Operational",
        contact = "Logistics Coordinator: 0917-555-0107",
        goodsType = "General Merchandise"
    ),
    Warehouse(
        id = "WH-08",
        name = "Nasugbu Cold Storage Facility",
        barangay = "Brgy. Pantalan, Nasugbu",
        latitude = 14.0720,
        longitude = 120.6275,
        capacity = "80% Full (5,000 pallets)",
        status = "Operational",
        contact = "Warehouse Manager: 0917-555-0108",
        goodsType = "Seafood, Meat & Frozen Goods"
    ),
    Warehouse(
        id = "WH-09",
        name = "Batangas Agri-Fishery Hub",
        barangay = "Brgy. Wawa, Nasugbu",
        latitude = 14.0815,
        longitude = 120.6190,
        capacity = "40% Full (3,500 pallets)",
        status = "Operational",
        contact = "Logistics Coordinator: 0917-555-0109",
        goodsType = "Agricultural Produce & Cold Storage"
    )
)

// ==========================================
// 3. MAIN ACTIVITY
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = "com.example.nasugbuwarehouselocator/1.1"

        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        setContent {
            MaterialTheme(colorScheme = AppBlueColorScheme) {
                WarehouseLocatorApp()
            }
        }
    }
}

// ==========================================
// 4. NAVIGATION & AUTH STATE CONTROLLER
// ==========================================
@Composable
fun WarehouseLocatorApp() {
    val context = LocalContext.current

    val registeredUsers = remember {
        mutableStateListOf(
            UserAccount("Admin Dispatcher", "admin@nasugbu.ph", "123456")
        )
    }

    var currentUser by remember { mutableStateOf<UserAccount?>(null) }
    var isSignUpScreen by remember { mutableStateOf(false) }

    if (currentUser == null) {
        if (isSignUpScreen) {
            SignUpScreen(
                onSignUpSuccess = { newUser ->
                    registeredUsers.add(newUser)
                    currentUser = newUser
                    Toast.makeText(context, "Account created! Welcome, ${newUser.fullName}", Toast.LENGTH_SHORT).show()
                },
                onNavigateToLogin = { isSignUpScreen = false }
            )
        } else {
            LoginScreen(
                registeredUsers = registeredUsers,
                onLoginSuccess = { user ->
                    currentUser = user
                    Toast.makeText(context, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()
                },
                onNavigateToSignUp = { isSignUpScreen = true }
            )
        }
    } else {
        MapDashboardScreen(
            user = currentUser!!,
            onLogout = {
                currentUser = null
                isSignUpScreen = false
                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==========================================
// 5. LOGIN SCREEN (BLUE THEME)
// ==========================================
@Composable
fun LoginScreen(
    registeredUsers: List<UserAccount>,
    onLoginSuccess: (UserAccount) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("admin@nasugbu.ph") }
    var password by remember { mutableStateOf("123456") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue, PrimaryBlue, BrightBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = LightBlueBg,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warehouse,
                        contentDescription = "Warehouse Logo",
                        tint = PrimaryBlue,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nasugbu Logistics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
                Text(
                    text = "Warehouse Distribution Locator",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val matchedUser = registeredUsers.find {
                            it.email.equals(email.trim(), ignoreCase = true) && it.password == password
                        }
                        if (matchedUser != null) {
                            onLoginSuccess(matchedUser)
                        } else {
                            errorMessage = "Invalid email or password."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("LOGIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Wala pang account? ", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "Sign Up",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. SIGN UP SCREEN (BLUE THEME)
// ==========================================
@Composable
fun SignUpScreen(
    onSignUpSuccess: (UserAccount) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlue, PrimaryBlue, BrightBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
                Text(
                    text = "Mag-register para ma-access ang Nasugbu Map",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = "" },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        when {
                            fullName.isBlank() || email.isBlank() || password.isBlank() -> {
                                errorMessage = "Pakipunan ang lahat ng fields."
                            }
                            password.length < 6 -> {
                                errorMessage = "Dapat hindi bababa sa 6 characters ang password."
                            }
                            password != confirmPassword -> {
                                errorMessage = "Hindi magkatugma ang password."
                            }
                            else -> {
                                onSignUpSuccess(UserAccount(fullName.trim(), email.trim(), password))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("SIGN UP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("May account na? ", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = "Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. MAP DASHBOARD SCREEN (NASUGBU, BATANGAS)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDashboardScreen(
    user: UserAccount,
    onLogout: () -> Unit
) {
    var selectedWarehouse by remember { mutableStateOf<Warehouse?>(nasugbuWarehouses.first()) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val nasugbuCenter = remember { GeoPoint(14.0750, 120.6650) }

    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nasugbu Warehouse Map",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "User: ${user.fullName}",
                            fontSize = 12.sp,
                            color = LightBlueBg
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlue
                ),
                actions = {
                    IconButton(onClick = {
                        mapViewRef?.controller?.animateTo(nasugbuCenter)
                        mapViewRef?.controller?.setZoom(14.5)
                    }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center Nasugbu",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        // PINALITAN NATIN ANG SERVER PARA HINDI MA-BLOCK!
                        // Gagamitin natin ang OPENTOPO map na libre din at makikita ang Nasugbu
                        setTileSource(TileSourceFactory.OpenTopo)
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true

                        minZoomLevel = 10.0
                        maxZoomLevel = 20.0
                        controller.setZoom(14.5)
                        controller.setCenter(nasugbuCenter)

                        nasugbuWarehouses.forEach { warehouse ->
                            val marker = Marker(this).apply {
                                position = GeoPoint(warehouse.latitude, warehouse.longitude)
                                title = warehouse.name
                                snippet = "${warehouse.barangay} • ${warehouse.goodsType}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { _, _ ->
                                    selectedWarehouse = warehouse
                                    controller.animateTo(position)
                                    controller.setZoom(15.5)
                                    showInfoWindow()
                                    true
                                }
                            }
                            overlays.add(marker)
                        }
                        mapViewRef = this
                    }
                }
            )

            LazyRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nasugbuWarehouses) { warehouse ->
                    val isSelected = selectedWarehouse?.id == warehouse.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedWarehouse = warehouse
                            val point = GeoPoint(warehouse.latitude, warehouse.longitude)
                            mapViewRef?.controller?.animateTo(point)
                            mapViewRef?.controller?.setZoom(15.5)
                        },
                        label = {
                            Text(
                                text = warehouse.barangay.removePrefix("Brgy. "),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = DeepBlue,
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            selectedWarehouse?.let { warehouse ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = LightBlueBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = warehouse.id,
                                    color = DeepBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = warehouse.status,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = warehouse.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )
                        Text(
                            text = warehouse.barangay,
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Goods Type", fontSize = 11.sp, color = Color.Gray)
                                Text(warehouse.goodsType, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Capacity", fontSize = 11.sp, color = Color.Gray)
                                Text(warehouse.capacity, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Contact: ${warehouse.contact}",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}
