class GeoCoordinate(
    val latitude: Double,
    val longitude: Double
)

abstract class BaseWarehouse(
    val warehouseId: String,
    val warehouseName: String,
    val location: GeoCoordinate,
    val operatingHours: String,
    val coverageRadiusKm: Double
) {
    abstract fun calculateDeliveryZone(): List<GeoCoordinate>
    
    open fun isWithinCoverage(destinationLocation: GeoCoordinate): Boolean {
        return true
    }
}

class CentralWarehouse(
    warehouseId: String,
    warehouseName: String,
    location: GeoCoordinate,
    operatingHours: String = "24/6",
    coverageRadiusKm: Double = 50.0,
    val dockCount: Int,
    val hasHeavyLifters: Boolean = true,
    val storageCapacityPallets: Int
) : BaseWarehouse(warehouseId, warehouseName, location, operatingHours, coverageRadiusKm) {
    override fun calculateDeliveryZone(): List<GeoCoordinate> {
        //  delivery hubs o coordinates
        return listOf(location)
    }
}

class FulfillmentCenter(
    warehouseId: String,
    warehouseName: String,
    location: GeoCoordinate,
    operatingHours: String = "24/7",
    coverageRadiusKm: Double = 25.0,
    val packingStationCount: Int,
    val hasSameDayShipping: Boolean = true,
    val supportedPlatform: String = "E-Commerce"
) : BaseWarehouse(warehouseId, warehouseName, location, operatingHours, coverageRadiusKm) {
    override fun calculateDeliveryZone(): List<GeoCoordinate> {
        return listOf(location)
    }
}

class ColdStorageWarehouse(
    warehouseId: String,
    warehouseName: String,
    location: GeoCoordinate,
    operatingHours: String = "24/7",
    coverageRadiusKm: Double = 15.0,
    val temperatureRangeCelsius: String, // hal. "-20°C to 4°C"
    val hasBlastFreezer: Boolean = true
) : BaseWarehouse(warehouseId, warehouseName, location, operatingHours, coverageRadiusKm) {
    override fun calculateDeliveryZone(): List<GeoCoordinate> {
        return listOf(location)
    }
}
