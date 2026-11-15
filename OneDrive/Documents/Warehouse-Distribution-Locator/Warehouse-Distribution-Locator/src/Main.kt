import kotlin.math.*

// ============================================================================
// 1. GEO-COORDINATE & HAVERSINE DISTANCE ENGINE
// ============================================================================

/**
 * Represents a geographical coordinate (Latitude and Longitude).
 */
class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calculates the distance in kilometers between two geographic coordinates using the Haversine formula.
     */
    fun distanceToKm(other: GeoCoordinate): Double {
        val radiusKm = 6371.0 // Radius of Earth in kilometers
        val dLat = Math.toRadians(other.latitude - this.latitude)
        val dLon = Math.toRadians(other.longitude - this.longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radiusKm * c
    }
}

// ============================================================================
// 2. ABSTRACT BASE WAREHOUSE
// ============================================================================

/**
 * Base abstract class for all warehouses in the distribution locator system.
 */
abstract class BaseWarehouse(
    val id: String,
    val warehouseName: String,
    val barangay: String,
    val location: GeoCoordinate,
    val operatingHours: String,
    val coverageRadiusKm: Double,
    val usedPallets: Int,
    val maxPallets: Int
) {
    abstract fun calculateDeliveryZone(): List<GeoCoordinate>
    abstract fun getTechnicalSpecs(): String

    val utilizationPercent: Int
        get() = if (maxPallets > 0) ((usedPallets.toDouble() / maxPallets) * 100).roundToInt() else 0

    open fun isWithinCoverage(destination: GeoCoordinate): Boolean {
        return location.distanceToKm(destination) <= coverageRadiusKm
    }
}

// ============================================================================
// 3. SPECIALIZED NASUGBU WAREHOUSE SUBCLASSES (STRICTLY IN NASUGBU)
// ============================================================================

/**
 * 1. Nasugbu Central Logistics Hub (Brgy. Poblacion, Nasugbu)
 * Coordinates: 14.0718° N, 120.6322° E
 */
class NasugbuCentralLogisticsHub(
    id: String = "NSG-CENTRAL-01",
    warehouseName: String = "Nasugbu Central Logistics Hub",
    barangay: String = "Brgy. Poblacion, Nasugbu",
    location: GeoCoordinate = GeoCoordinate(14.0718, 120.6322),
    operatingHours: String = "24/7 Operations",
    coverageRadiusKm: Double = 5.0,
    usedPallets: Int = 3700,
    maxPallets: Int = 5000,
    val heavyTruckBays: Int = 12
) : BaseWarehouse(id, warehouseName, barangay, location, operatingHours, coverageRadiusKm, usedPallets, maxPallets) {

    override fun calculateDeliveryZone(): List<GeoCoordinate> = listOf(location)
    override fun getTechnicalSpecs(): String = "Main Hub • $heavyTruckBays Heavy Truck Bays • Capacity: $maxPallets Pallets"

    fun dispatchHeavyTruck(destinationBarangay: String, pallets: Int): String {
        return "Dispatched heavy truck carrying $pallets pallets from $warehouseName to $destinationBarangay."
    }
}

/**
 * 2. Wawa Port Cold Chain Facility (Brgy. Wawa, Nasugbu)
 * Coordinates: 14.0842° N, 120.6195° E
 */
class NasugbuPortColdChainFacility(
    id: String = "NSG-COLD-02",
    warehouseName: String = "Wawa Port Cold Chain Facility",
    barangay: String = "Brgy. Wawa, Nasugbu",
    location: GeoCoordinate = GeoCoordinate(14.0842, 120.6195),
    operatingHours: String = "04:00 AM – 10:00 PM",
    coverageRadiusKm: Double = 4.0,
    usedPallets: Int = 1936,
    maxPallets: Int = 2200,
    val tempCelsius: Double = -18.5
) : BaseWarehouse(id, warehouseName, barangay, location, operatingHours, coverageRadiusKm, usedPallets, maxPallets) {

    override fun calculateDeliveryZone(): List<GeoCoordinate> = listOf(location)
    override fun getTechnicalSpecs(): String = "Cold Storage • Temperature maintained at ${tempCelsius}°C"

    fun checkSeafoodColdStorage(): String {
        return "$warehouseName Status: Temperature maintained at ${tempCelsius}°C. Storage capacity: $utilizationPercent% full."
    }
}

/**
 * 3. Natipuan Coastal Resort Supply Hub (Brgy. Natipuan, Nasugbu)
 * Coordinates: 14.1325° N, 120.6148° E
 */
class NasugbuResortSupplyFulfillment(
    id: String = "NSG-FULFILL-03",
    warehouseName: String = "Natipuan Coastal Resort Supply Hub",
    barangay: String = "Brgy. Natipuan, Nasugbu",
    location: GeoCoordinate = GeoCoordinate(14.1325, 120.6148),
    operatingHours: String = "06:00 AM – 09:00 PM",
    coverageRadiusKm: Double = 4.5,
    usedPallets: Int = 1984,
    maxPallets: Int = 3200,
    val dailyParcelCap: Int = 1450
) : BaseWarehouse(id, warehouseName, barangay, location, operatingHours, coverageRadiusKm, usedPallets, maxPallets) {

    override fun calculateDeliveryZone(): List<GeoCoordinate> = listOf(location)
    override fun getTechnicalSpecs(): String = "Last-Mile Resort Fulfillment • $dailyParcelCap Parcels/Day"

    fun estimateResortDeliveryTime(resortLocation: GeoCoordinate): String {
        val dist = location.distanceToKm(resortLocation)
        return if (isWithinCoverage(resortLocation)) {
            val mins = (dist * 4 + 10).toInt()
            "Estimated delivery time to resort: ~$mins mins (${"%.2f".format(dist)} km distance)"
        } else {
            "Destination resort is outside the $coverageRadiusKm km express delivery radius."
        }
    }
}

/**
 * 4. Aga Highland Agro-Distribution Center (Brgy. Aga, Nasugbu)
 * Coordinates: 14.1012° N, 120.7285° E
 */
class NasugbuAgroDistributionCenter(
    id: String = "NSG-AGRO-04",
    warehouseName: String = "Aga Highland Agro-Distribution Center",
    barangay: String = "Brgy. Aga, Nasugbu",
    location: GeoCoordinate = GeoCoordinate(14.1012, 120.7285),
    operatingHours: String = "24/7 Cross-Docking",
    coverageRadiusKm: Double = 6.0,
    usedPallets: Int = 2610,
    maxPallets: Int = 4500,
    val cropTypesHandled: List<String> = listOf("Sugarcane", "Fertilizers", "Highland Produce")
) : BaseWarehouse(id, warehouseName, barangay, location, operatingHours, coverageRadiusKm, usedPallets, maxPallets) {

    override fun calculateDeliveryZone(): List<GeoCoordinate> = listOf(location)
    override fun getTechnicalSpecs(): String = "Agro Distribution • Crops: ${cropTypesHandled.joinToString(", ")}"

    fun dispatchAgriProduce(cropName: String, destination: String): String {
        return "Dispatched shipment of $cropName from $warehouseName to $destination."
    }
}

/**
 * 5. Lumbangan South Border Depot (Brgy. Lumbangan, Nasugbu)
 * Coordinates: 14.0345° N, 120.6480° E
 */
class NasugbuBorderDepot(
    id: String = "NSG-BORDER-05",
    warehouseName: String = "Lumbangan South Border Depot",
    barangay: String = "Brgy. Lumbangan, Nasugbu",
    location: GeoCoordinate = GeoCoordinate(14.0345, 120.6480),
    operatingHours: String = "07:00 AM – 07:00 PM",
    coverageRadiusKm: Double = 4.0,
    usedPallets: Int = 875,
    maxPallets: Int = 2500
) : BaseWarehouse(id, warehouseName, barangay, location, operatingHours, coverageRadiusKm, usedPallets, maxPallets) {

    override fun calculateDeliveryZone(): List<GeoCoordinate> = listOf(location)
    override fun getTechnicalSpecs(): String = "South Gateway Depot • Located near Nasugbu-Lian Border"
}

// ============================================================================
// 4. NASUGBU WAREHOUSE LOCATOR ENGINE & LOCATOR FUNCTIONS
// ============================================================================

class NasugbuWarehouseLocator(
    // Contains ALL warehouses strictly located in Nasugbu, Batangas
    val nasugbuWarehouses: List<BaseWarehouse> = listOf(
        NasugbuCentralLogisticsHub(),
        NasugbuPortColdChainFacility(),
        NasugbuResortSupplyFulfillment(),
        NasugbuAgroDistributionCenter(),
        NasugbuBorderDepot()
    )
) {
    /**
     * Function 1: Retrieves all warehouses located in Nasugbu.
     */
    fun locateAllNasugbuWarehouses(): List<BaseWarehouse> = nasugbuWarehouses

    /**
     * Function 2: Finds the nearest Nasugbu warehouse to a specific coordinate.
     */
    fun findNearestNasugbuWarehouse(targetLocation: GeoCoordinate): Pair<BaseWarehouse, Double>? {
        return nasugbuWarehouses.map { warehouse ->
            warehouse to warehouse.location.distanceToKm(targetLocation)
        }.minByOrNull { it.second }
    }

    /**
     * Function 3: Finds all Nasugbu warehouses whose coverage radius covers a specific coordinate.
     */
    fun findNasugbuWarehousesInCoverage(targetLocation: GeoCoordinate): List<BaseWarehouse> {
        return nasugbuWarehouses.filter { warehouse ->
            warehouse.isWithinCoverage(targetLocation)
        }
    }

    /**
     * Function 4: Filters warehouses by specific barangay within Nasugbu.
     */
    fun filterByBarangay(barangayName: String): List<BaseWarehouse> {
        return nasugbuWarehouses.filter { warehouse ->
            warehouse.barangay.contains(barangayName, ignoreCase = true)
        }
    }

    /**
     * Function 5: Displays a complete summary report table of all Nasugbu warehouses relative to a customer coordinate.
     */
    fun displayNasugbuWarehouseSummary(userLocation: GeoCoordinate) {
        println("=============================================================================================")
        println("                       ALL WAREHOUSES LOCATED IN NASUGBU, BATANGAS                           ")
        println("=============================================================================================")
        printf("%-15s | %-36s | %-25s | %-10s | %-8s\n", "ID", "WAREHOUSE NAME", "BARANGAY", "DISTANCE", "LOAD %")
        println("---------------------------------------------------------------------------------------------")

        nasugbuWarehouses.forEach { wh ->
            val dist = wh.location.distanceToKm(userLocation)
            printf("%-15s | %-36s | %-25s | %-9.2f km | %-8d%%\n",
                wh.id, wh.warehouseName, wh.barangay, dist, wh.utilizationPercent
            )
        }
        println("=============================================================================================\n")
    }
}

// ============================================================================
// 5. MAIN DEMO EXECUTION
// ============================================================================

fun main() {
    val locator = NasugbuWarehouseLocator()

    // Sample customer location at Brgy. Bucana, Nasugbu (14.0585, 120.6250)
    val customerLocation = GeoCoordinate(14.0585, 120.6250)

    println("=== NASUGBU WAREHOUSE DISTRIBUTION LOCATOR ===")

    // 1. Locate ALL warehouses in Nasugbu
    println("\nAll Warehouses Located in Nasugbu (${locator.locateAllNasugbuWarehouses().size} Total Hubs):")
    locator.locateAllNasugbuWarehouses().forEach { wh ->
        println(" - [${wh.id}] ${wh.warehouseName} (${wh.barangay}) -> Coords: ${wh.location.latitude}, ${wh.location.longitude}")
    }

    // 2. Display summary table relative to customer location
    println("\n")
    locator.displayNasugbuWarehouseSummary(customerLocation)

    // 3. Find the nearest warehouse in Nasugbu
    val nearestResult = locator.findNearestNasugbuWarehouse(customerLocation)
    if (nearestResult != null) {
        val (warehouse, distance) = nearestResult
        println("Nearest Warehouse to Customer:")
        println("   Name        : ${warehouse.warehouseName}")
        println("   Barangay    : ${warehouse.barangay}")
        println("   Distance    : ${"%.2f".format(distance)} km")
        println("   Tech Specs  : ${warehouse.getTechnicalSpecs()}")
    }

    // 4. Test subclass-specific functions
    println("\n--- SUBCLASS-SPECIFIC FUNCTIONS DEMO ---")
    val coldStorage = locator.locateAllNasugbuWarehouses().filterIsInstance<NasugbuPortColdChainFacility>().first()
    println(coldStorage.checkSeafoodColdStorage())

    val resortHub = locator.locateAllNasugbuWarehouses().filterIsInstance<NasugbuResortSupplyFulfillment>().first()
    println(resortHub.estimateResortDeliveryTime(customerLocation))

    val centralHub = locator.locateAllNasugbuWarehouses().filterIsInstance<NasugbuCentralLogisticsHub>().first()
    println(centralHub.dispatchHeavyTruck("Brgy. Wawa", 50))
}
