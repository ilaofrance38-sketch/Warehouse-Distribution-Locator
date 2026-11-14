package com.nasugbu.warehouselocator

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.html.*

// ============================================================================
// 1. DOMAIN MODELS & NASUGBU WAREHOUSE DATA
// ============================================================================

enum class WarehouseType(val label: String, val colorHex: String) {
    CENTRAL("Central Warehouse", "#10B981"),
    FULFILLMENT("Fulfillment Center", "#3B82F6"),
    COLD_STORAGE("Cold Storage", "#06B6D4")
}

enum class WarehouseStatus(val label: String, val badgeColor: String) {
    ACTIVE("Active", "bg-emerald-500/20 text-emerald-400 border-emerald-500/30"),
    NEAR_CAPACITY("Near Capacity", "bg-amber-500/20 text-amber-400 border-amber-500/30"),
    MAINTENANCE("Maintenance", "bg-rose-500/20 text-rose-400 border-rose-500/30")
}

data class Warehouse(
    val id: String,
    val name: String,
    val barangay: String,
    val latitude: Double,
    val longitude: Double,
    val usedPallets: Int,
    val maxPallets: Int,
    val coverageRadiusKm: Double,
    val type: WarehouseType,
    val status: WarehouseStatus,
    val manager: String,
    val hotline: String,
    val hours: String,
    val skus: List<String>
) {
    val utilizationPercent: Int
        get() = if (maxPallets > 0) ((usedPallets.toDouble() / maxPallets) * 100).toInt() else 0
}

object NasugbuWarehouseRepository {
    fun getWarehouses(): List<Warehouse> = listOf(
        Warehouse(
            id = "NSG-01",
            name = "Nasugbu Central Logistics Hub",
            barangay = "Brgy. Poblacion",
            latitude = 14.0718,
            longitude = 120.6322,
            usedPallets = 3700,
            maxPallets = 5000,
            coverageRadiusKm = 4.5,
            type = WarehouseType.CENTRAL,
            status = WarehouseStatus.ACTIVE,
            manager = "Engr. Mateo Mendoza",
            hotline = "(043) 416-2201",
            hours = "24/7 Operations",
            skus = listOf("FMCG", "Rice & Grains", "Hardware", "Beverages")
        ),
        Warehouse(
            id = "NSG-02",
            name = "Wawa Port Cold Chain Facility",
            barangay = "Brgy. Wawa",
            latitude = 14.0842,
            longitude = 120.6195,
            usedPallets = 1936,
            maxPallets = 2200,
            coverageRadiusKm = 3.5,
            type = WarehouseType.COLD_STORAGE,
            status = WarehouseStatus.NEAR_CAPACITY,
            manager = "Clarissa Dimaano",
            hotline = "(043) 416-3109",
            hours = "04:00 AM – 10:00 PM",
            skus = listOf("Fresh Seafood", "Frozen Meat", "Dairy", "Ice Blocks")
        ),
        Warehouse(
            id = "NSG-03",
            name = "Natipuan Coastal Resort Supply Hub",
            barangay = "Brgy. Natipuan",
            latitude = 14.1325,
            longitude = 120.6148,
            usedPallets = 1984,
            maxPallets = 3200,
            coverageRadiusKm = 4.0,
            type = WarehouseType.FULFILLMENT,
            status = WarehouseStatus.ACTIVE,
            manager = "Jomar Villalobos",
            hotline = "(043) 416-8840",
            hours = "06:00 AM – 09:00 PM",
            skus = listOf("Hotel Linens", "Bottled Water", "Resort Amenities")
        ),
        Warehouse(
            id = "NSG-04",
            name = "Aga Highland Agro-Distribution Center",
            barangay = "Brgy. Aga",
            latitude = 14.1012,
            longitude = 120.7285,
            usedPallets = 2610,
            maxPallets = 4500,
            coverageRadiusKm = 5.0,
            type = WarehouseType.CENTRAL,
            status = WarehouseStatus.ACTIVE,
            manager = "Ramonito Laurel",
            hotline = "(043) 416-5512",
            hours = "24/7 Cross-Docking",
            skus = listOf("Sugarcane Supplies", "Fertilizers", "Highland Produce")
        ),
        Warehouse(
            id = "NSG-05",
            name = "Kayrilaw East Gateway Cross-Dock",
            barangay = "Brgy. Kayrilaw",
            latitude = 14.1288,
            longitude = 120.7610,
            usedPallets = 1470,
            maxPallets = 3000,
            coverageRadiusKm = 4.2,
            type = WarehouseType.FULFILLMENT,
            status = WarehouseStatus.ACTIVE,
            manager = "Bea Marasigan",
            hotline = "(043) 416-9023",
            hours = "05:00 AM – 10:00 PM",
            skus = listOf("E-Commerce Parcels", "Retail Restock", "Auto Parts")
        ),
        Warehouse(
            id = "NSG-06",
            name = "Bucana Marine & Agri Chiller",
            barangay = "Brgy. Bucana",
            latitude = 14.0585,
            longitude = 120.6250,
            usedPallets = 1638,
            maxPallets = 1800,
            coverageRadiusKm = 3.0,
            type = WarehouseType.COLD_STORAGE,
            status = WarehouseStatus.NEAR_CAPACITY,
            manager = "Capt. Dante Macatangay",
            hotline = "(043) 416-4478",
            hours = "04:30 AM – 09:00 PM",
            skus = listOf("Tuna & Catch", "Poultry", "Chilled Produce")
        )
    )
}

// ============================================================================
// 2. KTOR SERVER SETUP & HTML RENDERING
// ============================================================================

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                val warehouses = NasugbuWarehouseRepository.getWarehouses()
                call.respondHtml {
                    head {
                        title = "Nasugbu Warehouse Distribution Locator"
                        script(src = "https://cdn.tailwindcss.com") {}
                        // Leaflet CSS & JS for OpenStreetMap integration
                        link(rel = "stylesheet", href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css") {}
                        script(src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js") {}
                    }
                    body(classes = "bg-slate-950 text-slate-100 min-h-screen font-sans") {
                        // Header Navigation
                        header(classes = "bg-slate-900 border-b border-slate-800 px-6 py-4 flex justify-between items-center") {
                            div(classes = "flex items-center space-x-3") {
                                div(classes = "w-10 h-10 rounded-xl bg-emerald-500/10 border border-emerald-500 flex items-center justify-center text-emerald-400 font-bold") {
                                    + "📍"
                                }
                                div {
                                    h1(classes = "text-lg font-extrabold tracking-wide") { + "NASUGBU LOGISTICS GRID" }
                                    p(classes = "text-xs text-slate-400") { + "Batangas Warehouse & Distribution Locator" }
                                }
                            }
                            div(classes = "text-xs px-3 py-1.5 rounded-full bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 font-semibold") {
                                + "🟢 Live Grid Status: Operational"
                            }
                        }

                        // Main Content Workspace
                        main(classes = "p-6 max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-6") {
                            
                            // Left Column: Map Container (2 Spans wide)
                            div(classes = "lg:col-span-2 flex flex-col space-y-4") {
                                div(classes = "bg-slate-900 border border-slate-800 rounded-2xl p-4 shadow-xl") {
                                    div(classes = "flex justify-between items-center mb-3") {
                                        h2(classes = "text-sm font-bold uppercase tracking-wider text-slate-300") { + "Nasugbu Geographic Distribution Map" }
                                        span(classes = "text-xs text-slate-500") { + "Center: Poblacion / Wawa Corridor" }
                                    }
                                    // Leaflet Map Div
                                    div(classes = "w-full h-[450px] rounded-xl border border-slate-800 z-10") {
                                        id = "nasugbuMap"
                                    }
                                }
                            }

                            // Right Column: KPI Metrics & Hub Summary
                            div(classes = "flex flex-col space-y-4") {
                                // KPI Card
                                div(classes = "bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl") {
                                    h3(classes = "text-xs font-bold uppercase tracking-wider text-slate-400 mb-4") { + "Nasugbu Grid Metrics" }
                                    div(classes = "grid grid-cols-2 gap-4") {
                                        div(classes = "bg-slate-950 p-3 rounded-xl border border-slate-800") {
                                            p(classes = "text-xs text-slate-500") { + "Active Hubs" }
                                            p(classes = "text-xl font-extrabold text-emerald-400") { 
                                                + "${warehouses.count { it.status == WarehouseStatus.ACTIVE }}/${warehouses.size}" 
                                            }
                                        }
                                        div(classes = "bg-slate-950 p-3 rounded-xl border border-slate-800") {
                                            p(classes = "text-xs text-slate-500") { + "Avg Utilization" }
                                            val avg = if(warehouses.isNotEmpty()) warehouses.map { it.utilizationPercent }.average().toInt() else 0
                                            p(classes = "text-xl font-extrabold text-amber-400") { + "$avg%" }
                                        }
                                    }
                                }

                                // Hub List Quick View
                                div(classes = "bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl flex-1") {
                                    h3(classes = "text-xs font-bold uppercase tracking-wider text-slate-400 mb-3") { + "Registered Warehouses" }
                                    div(classes = "space-y-3 max-h-[380px] overflow-y-auto pr-1") {
                                        warehouses.forEach { wh ->
                                            div(classes = "p-3 rounded-xl bg-slate-950 border border-slate-800/80 hover:border-slate-700 transition") {
                                                div(classes = "flex justify-between items-start") {
                                                    span(classes = "text-xs font-bold text-white") { + wh.name }
                                                    span(classes = "text-[10px] px-2 py-0.5 rounded border ${wh.status.badgeColor}") { + wh.status.label }
                                                }
                                                p(classes = "text-xs text-slate-400 mt-1") { + "${wh.barangay}, Nasugbu • ID: ${wh.id}" }
                                                p(classes = "text-[11px] text-emerald-400 mt-2 font-medium") { + "Manager: ${wh.manager}" }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Leaflet Map Initialization Script
                        script {
                            unsafe {
                                raw("""
                                    document.addEventListener("DOMContentLoaded", function() {
                                        // Initialize map centered on Nasugbu, Batangas
                                        var map = L.map('nasugbuMap').setView([14.0725, 120.6325], 13);

                                        // Use dark-themed map tiles for logistics dashboard styling
                                        L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
                                            attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
                                            maxZoom: 19
                                        }).addTo(map);

                                        // Warehouse data passed from Ktor backend
                                        var hubs = [
                                            ${warehouses.joinToString(",") { wh ->
                                                "{id:'${wh.id}', name:'${wh.name}', barangay:'${wh.barangay}', lat:${wh.latitude}, lng:${wh.longitude}, util:${wh.utilizationPercent}, type:'${wh.type.label}', color:'${wh.type.colorHex}'}"
                                            }}
                                        ];

                                        hubs.forEach(function(hub) {
                                            // Add custom circle marker
                                            var marker = L.circleMarker([hub.lat, hub.lng], {
                                                radius: 8,
                                                fillColor: hub.color,
                                                color: "#ffffff",
                                                weight: 2,
                                                opacity: 1,
                                                fillOpacity: 0.9
                                            }).addTo(map);

                                            marker.bindPopup("<b>" + hub.name + "</b><br>" + hub.barangay + "<br>Type: " + hub.type + "<br>Utilization: " + hub.util + "%");
                                        });
                                    });
                                """)
                            }
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}
