// Abstract class for Warehouse Distribution Locator
abstract class Warehouse(
    val warehouseId: String,
    var warehouseName: String,
    protected var latitude: Double,
    protected var longitude: Double,
    private var capacity: Int
) {
    // Abstract methods (need override subclasses)
    abstract fun getWarehouseType(): String
    abstract fun getDetails(): String

    // Common method: coordinates
    fun coordinates(): String = "$latitude, $longitude"

    // Common method: check capacity
    fun hasCapacity(units: Int): Boolean = units <= capacity
}

// Subclass: Regional Warehouse
class RegionalWarehouse(
    warehouseId: String,
    warehouseName: String,
    latitude: Double,
    longitude: Double,
    capacity: Int
) : Warehouse(warehouseId, warehouseName, latitude, longitude, capacity) {
    override fun getWarehouseType(): String {
        return "Regional Distribution Center"
    }

    override fun getDetails(): String {
        return "Warehouse: $ware
    }
