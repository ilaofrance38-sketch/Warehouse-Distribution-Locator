import android.content.Context
import android.content.SharedPreferences
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ======================================================================================
// 1. WAREHOUSE DISTRIBUTION LOCATOR: DATA MODELS & ENUMS
// ======================================================================================

enum class WarehouseTier(val label: String, val hexColor: Long, val badgeBg: Long, val badgeBorder: Long) {
    PRIMARY_HUB("Primary Hub", 0xFF1E3A8A, 0xFFEFF6FF, 0xFFBFDBFE),      // National/Regional Mega-Center
    REGIONAL_DC("Regional DC", 0xFF047857, 0xFFECFDF5, 0xFFA7F3D0),      // Provincial Distribution Center
    SATELLITE_DEPOT("Satellite Depot", 0xFFB45309, 0xFFFFFBEB, 0xFFFDE68A) // Last-mile cross-dock station
}

data class ServiceZone(
    val id: String,
    val name: String,
    val regionType: String,
    val lat: Double,
    val lng: Double
)

data class WarehouseFacility(
    val id: String,
    val tier: WarehouseTier,
    val codeName: String,
    val zoneId: String,
    val lat: Double,
    val lng: Double,
    val coverageRadiusKm: Double,
    val operatingHours: String,
    val isOperational: Boolean,
    val capabilities: List<String>,
    val logisticsNotes: String
)

data class FacilityProximityResult(
    val facility: WarehouseFacility,
    val distanceKm: Double
)

data class WarehouseUserAccount(
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val role: String
)

// ======================================================================================
// 2. WAREHOUSE DISTRIBUTION LOCATOR: AUTHENTICATION & SESSION MANAGER
// ======================================================================================

class WarehouseAuthManager(context: Context) {
    // SharedPreferences pangalan na nakatutok sa Warehouse Distribution Locator app
    private val prefs: SharedPreferences = context.getSharedPreferences("warehouse_distribution_locator_auth", Context.MODE_PRIVATE)

    sealed class AuthResult {
        data class Success(val account: WarehouseUserAccount) : AuthResult()
        object AccountNotFound : AuthResult()
        object IncorrectPassword : AuthResult()
    }

    init {
        if (!accountExists("admin@warehouselocator.com")) {
            registerAccount("Warehouse System Administrator", "admin@warehouselocator.com", "admin123", "Fleet Manager")
        }
    }

    fun accountExists(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        return prefs.contains("user_${cleanEmail}_pass")
    }

    fun registerAccount(fullName: String, email: String, pass: String, role: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || pass.length < 6) return false
        val key = "user_$cleanEmail"
        if (accountExists(cleanEmail)) return false

        prefs.edit()
            .putString("${key}_name", fullName.trim())
            .putString("${key}_pass", pass)
            .putString("${key}_role", role)
            .putLong("${key}_created_at", System.currentTimeMillis())
            .putBoolean("${key}_tutorial_seen", false)
            .commit()
        return true
    }

    fun login(email: String, pass: String): AuthResult {
        val cleanEmail = email.trim().lowercase()
        val key = "user_$cleanEmail"
        if (!accountExists(cleanEmail)) {
            return AuthResult.AccountNotFound
        }
        val storedPass = prefs.getString("${key}_pass", null)
        if (storedPass == pass) {
            val name = prefs.getString("${key}_name", "Verified Dispatcher") ?: "Verified Dispatcher"
            val role = prefs.getString("${key}_role", "Operator") ?: "Operator"
            saveSession(name, cleanEmail, role)
            return AuthResult.Success(WarehouseUserAccount(name, cleanEmail, storedPass, role))
        }
        return AuthResult.IncorrectPassword
    }

    fun authenticate(email: String, pass: String): WarehouseUserAccount? {
        val result = login(email, pass)
        return if (result is AuthResult.Success) result.account else null
    }

    fun saveSession(fullName: String, email: String, role: String) {
        prefs.edit()
            .putString("session_name", fullName)
            .putString("session_email", email)
            .putString("session_role", role)
            .commit()
    }

    fun clearSession() {
        prefs.edit()
            .remove("session_name")
            .remove("session_email")
            .remove("session_role")
            .commit()
    }

    fun getCurrentSession(): WarehouseUserAccount? {
        val email = prefs.getString("session_email", null) ?: return null
        val name = prefs.getString("session_name", "Dispatcher") ?: "Dispatcher"
        val role = prefs.getString("session_role", "Operator") ?: "Operator"
        return WarehouseUserAccount(name, email, "", role)
    }
}

// ======================================================================================
// 3. WAREHOUSE DISTRIBUTION LOCATOR: DATA ENGINE (ZONES & FACILITIES)
// ======================================================================================

object WarehouseDistributionDataStore {
    private const val EARTH_RADIUS_KM = 6371.0088

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    val serviceZones: List<ServiceZone> by lazy {
        val list = mutableListOf<ServiceZone>()

        // 12 Urban / Commercial Hub Zones
        for (i in 0 until 12) {
            val row = (i / 4) - 1
            val col = (i % 4) - 1.5
            list.add(
                ServiceZone(
                    id = "zone-${i + 1}",
                    name = "Commercial Sector ${i + 1} (Urban Hub)",
                    regionType = "Metro Urban",
                    lat = 14.0720 + row * 0.0032,
                    lng = 120.6320 + col * 0.0035
                )
            )
        }

        // 30 Regional, Industrial & Coastal Supply Zones
        val regionalZoneList = listOf(
            Triple("Aga Industrial Park", 14.07449, 120.73847),
            Triple("Balaytigui Logistics Corridor", 14.13752, 120.61198),
            Triple("Banilad Commercial Zone", 14.0640, 120.6520),
            Triple("Bilaran Depot Zone", 14.0500, 120.6430),
            Triple("Bucana Port Logistics", 14.09312, 120.62887),
            Triple("Bulihan Supply District", 14.0830, 120.6560),
            Triple("Bunducan Terminal", 14.1090, 120.6050),
            Triple("Butucan District", 14.0450, 120.6560),
            Triple("Calayo Coastal Hub", 14.18631, 120.60942),
            Triple("Catandaan Sector", 14.1170, 120.6680),
            Triple("Cogunan Outpost", 14.0350, 120.6380),
            Triple("Dayap Freight Way", 14.08861, 120.74834),
            Triple("Kaylaway Expressway Depot", 14.06731, 120.78519),
            Triple("Kayrilaw Zone", 14.0900, 120.6520),
            Triple("Latag Industrial Strip", 14.1150, 120.6350),
            Triple("Looc Maritime Dock", 14.22178, 120.63276),
            Triple("Lumbangan Supply Route", 14.0760, 120.6830),
            Triple("Malapad Na Bato Sector", 14.1240, 120.6640),
            Triple("Mataas Na Pulo Freight", 14.1300, 120.5850),
            Triple("Maugat Junction", 14.0420, 120.6700),
            Triple("Munting Indan Terminal", 14.0700, 120.6150),
            Triple("Natipuan Agro Hub", 14.0980, 120.7480),
            Triple("Pantalan Port Area", 14.0780, 120.6180),
            Triple("Papaya District", 14.0210, 120.6200),
            Triple("Putat Sector", 14.0280, 120.6240),
            Triple("Reparo Logistics Yard", 14.0710, 120.6950),
            Triple("Talangan Station", 14.0560, 120.6900),
            Triple("Tumalim Cargo Yard", 14.0300, 120.6050),
            Triple("Utod Route Point", 14.0390, 120.6150),
            Triple("Wawa Harbor Terminal", 14.07592, 120.62248)
        )

        for ((name, lat, lng) in regionalZoneList) {
            list.add(
                ServiceZone(
                    id = name.lowercase().replace(" ", "-"),
                    name = name,
                    regionType = "Industrial / Rural District",
                    lat = lat,
                    lng = lng
                )
            )
        }

        list
    }

    // Official Warehouse Distribution Centers Database
    val facilities: List<WarehouseFacility> by lazy {
        listOf(
            // === PRIMARY REGIONAL HUBS ===
            WarehouseFacility(
                id = "WDL-HUB-01",
                tier = WarehouseTier.PRIMARY_HUB,
                codeName = "Warehouse Distribution Locator - National South Mega Hub",
                zoneId = "aga",
                lat = 14.07449,
                lng = 120.73847,
                coverageRadiusKm = 15.0,
                operatingHours = "24/7 Automated Operations",
                isOperational = true,
                capabilities = listOf("Automated Sorting", "Cold Chain Storage", "Cross-Docking", "Heavy Fleet Parking"),
                logisticsNotes = "Main regional center managed under Warehouse Distribution Locator network."
            ),
            WarehouseFacility(
                id = "WDL-HUB-02",
                tier = WarehouseTier.PRIMARY_HUB,
                codeName = "Warehouse Distribution Locator - Kaylaway Gateway",
                zoneId = "kaylaway",
                lat = 14.06731,
                lng = 120.78519,
                coverageRadiusKm = 12.0,
                operatingHours = "24/7 Operations",
                isOperational = true,
                capabilities = listOf("Pallet Racking", "Customs Bonded Area", "Express Sorting"),
                logisticsNotes = "High-capacity distribution interchange."
            ),

            // === REGIONAL DISTRIBUTION CENTERS (DCs) ===
            WarehouseFacility(
                id = "WDL-DC-001",
                tier = WarehouseTier.REGIONAL_DC,
                codeName = "Warehouse Distribution Locator - Poblacion Center",
                zoneId = "zone-2",
                lat = 14.07507,
                lng = 120.63259,
                coverageRadiusKm = 5.0,
                operatingHours = "4:00 AM - 12:00 AM",
                isOperational = true,
                capabilities = listOf("Quick-Pick Inventory", "Last-Mile Van Dispatch", "Ambient Storage"),
                logisticsNotes = "Central urban node for quick local fulfillment."
            ),
            WarehouseFacility(
                id = "WDL-DC-002",
                tier = WarehouseTier.REGIONAL_DC,
                codeName = "Warehouse Distribution Locator - Wawa Port DC",
                zoneId = "wawa",
                lat = 14.07943,
                lng = 120.62646,
                coverageRadiusKm = 6.0,
                operatingHours = "5:00 AM - 11:00 PM",
                isOperational = true,
                capabilities = listOf("Marine Freight", "Cold Storage", "Bulk Goods"),
                logisticsNotes = "Directly services coastal transport and sea-trade routes."
            ),
            WarehouseFacility(
                id = "WDL-DC-003",
                tier = WarehouseTier.REGIONAL_DC,
                codeName = "Warehouse Distribution Locator - Looc Depot",
                zoneId = "looc",
                lat = 14.22178,
                lng = 120.63276,
                coverageRadiusKm = 8.0,
                operatingHours = "6:00 AM - 10:00 PM",
                isOperational = true,
                capabilities = listOf("Resort Supply Staging", "Dry Goods", "Secure Locker"),
                logisticsNotes = "Northern regional fulfillment facility."
            ),

            // === SATELLITE DEPOTS ===
            WarehouseFacility(
                id = "WDL-DEPOT-001",
                tier = WarehouseTier.SATELLITE_DEPOT,
                codeName = "Warehouse Distribution Locator - J.P. Laurel Micro-Depot",
                zoneId = "zone-1",
                lat = 14.07212,
                lng = 120.63094,
                coverageRadiusKm = 2.5,
                operatingHours = "6:00 AM - 10:00 PM",
                isOperational = true,
                capabilities = listOf("Micro-Fulfillment", "Bike Courier Hub", "Same-Day Parcel Locker"),
                logisticsNotes = "Express dispatch outpost along main commercial avenue."
            )
        )
    }

    fun getServiceZoneName(id: String): String {
        return serviceZones.firstOrNull { it.id == id }?.name ?? "General Operations Sector"
    }

    fun findCoveringFacilities(lat: Double, lng: Double, list: List<WarehouseFacility>): List<FacilityProximityResult> {
        return list.map { facility ->
            val dist = haversineKm(lat, lng, facility.lat, facility.lng)
            FacilityProximityResult(facility, dist)
        }.filter { it.distanceKm <= it.facility.coverageRadiusKm }
            .sortedBy { it.distanceKm }
    }

    fun findAllFacilitiesWithDistance(lat: Double, lng: Double, list: List<WarehouseFacility>): List<FacilityProximityResult> {
        return list.map { facility ->
            val dist = haversineKm(lat, lng, facility.lat, facility.lng)
            FacilityProximityResult(facility, dist)
        }.sortedBy { it.distanceKm }
    }
}
