<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nasugbu Warehouse Map Locator</title>
    
    <!-- Tailwind CSS -->
    <script src="https://cdn.tailwindcss.com"></script>
    
    <!-- Google Fonts: Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    
    <!-- Leaflet.js CSS & JS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>

    <!-- Lucide Icons -->
    <script src="https://unpkg.com/lucide@latest"></script>

    <style>
        body {
            font-family: 'Inter', sans-serif;
            background-color: #f8fafc;
            color: #0f172a;
        }

        /* Custom Scrollbar */
        ::-webkit-scrollbar {
            width: 6px;
            height: 6px;
        }
        ::-webkit-scrollbar-track {
            background: #f1f5f9;
        }
        ::-webkit-scrollbar-thumb {
            background: #cbd5e1;
            border-radius: 9999px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: #94a3b8;
        }

        /* Crisp Light Mode Leaflet Popup Overrides */
        .leaflet-popup-content-wrapper, .leaflet-popup-tip {
            background-color: #ffffff !important;
            color: #0f172a !important;
            border: 1px solid #e2e8f0;
            border-radius: 14px !important;
            box-shadow: 0 12px 28px -6px rgba(15, 23, 42, 0.25) !important;
        }
        .leaflet-popup-close-button {
            color: #64748b !important;
            padding: 8px 10px 0 0 !important;
            font-size: 16px !important;
        }
        .leaflet-container {
            background-color: #e2e8f0 !important;
            font-family: 'Inter', sans-serif !important;
        }
        .leaflet-control-zoom a {
            background-color: #ffffff !important;
            color: #0f172a !important;
            border-color: #cbd5e1 !important;
            font-weight: 700 !important;
        }
        .leaflet-control-zoom a:hover {
            background-color: #f1f5f9 !important;
        }

        /* Permanent Crisp Warehouse & Barangay Labels */
        .warehouse-permanent-label {
            background: rgba(15, 23, 42, 0.92);
            color: #ffffff;
            border: 1.5px solid #ffffff;
            border-radius: 6px;
            padding: 2px 7px;
            font-size: 11px;
            font-weight: 700;
            white-space: nowrap;
            box-shadow: 0 3px 10px rgba(0, 0, 0, 0.35);
            letter-spacing: 0.01em;
        }

        .landmark-label {
            background: rgba(255, 255, 255, 0.92);
            color: #1e293b;
            border: 1px solid #94a3b8;
            border-radius: 5px;
            padding: 1px 6px;
            font-size: 10px;
            font-weight: 600;
            white-space: nowrap;
            box-shadow: 0 2px 6px rgba(15, 23, 42, 0.15);
        }

        /* Custom Marker Pulse Animation */
        @keyframes pulse-ring {
            0% { transform: scale(0.8); opacity: 0.85; }
            100% { transform: scale(2.3); opacity: 0; }
        }
        .marker-pulse {
            animation: pulse-ring 2s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
        }
    </style>
</head>
<body class="h-screen w-screen overflow-hidden flex flex-col select-none">

    <!-- ============================================================================ -->
    <!-- 0. SIGN UP & LOGIN AUTHENTICATION OVERLAY SCREEN                             -->
    <!-- ============================================================================ -->
    <div id="auth-screen" class="fixed inset-0 z-50 bg-slate-900/95 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto">
        <div class="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden my-auto">
            <!-- Auth Header Banner -->
            <div class="bg-gradient-to-r from-slate-900 via-slate-800 to-emerald-950 p-6 text-white relative overflow-hidden">
                <div class="flex items-center gap-3 relative z-10">
                    <div class="w-11 h-11 rounded-xl bg-emerald-500 text-slate-950 flex items-center justify-center shadow-lg">
                        <i data-lucide="map-pinned" class="w-6 h-6"></i>
                    </div>
                    <div>
                        <span class="text-[10px] font-bold uppercase tracking-widest text-emerald-400 block">Batangas Logistics Grid</span>
                        <h1 class="text-lg font-extrabold tracking-tight leading-tight">Nasugbu Warehouse Locator</h1>
                    </div>
                </div>
                <p class="text-xs text-slate-300 mt-3 relative z-10">
                    Sign in or create a logistics account to access live Nasugbu barangay maps, hub coverage zones, and dispatch controls.
                </p>
            </div>

            <!-- Auth Mode Tabs -->
            <div class="grid grid-cols-2 border-b border-slate-200 bg-slate-50">
                <button id="tab-login" onclick="switchAuthTab('login')" class="py-3 text-xs sm:text-sm font-bold text-emerald-700 border-b-2 border-emerald-600 bg-white transition">
                    Log In
                </button>
                <button id="tab-signup" onclick="switchAuthTab('signup')" class="py-3 text-xs sm:text-sm font-semibold text-slate-500 hover:text-slate-800 border-b-2 border-transparent transition">
                    Sign Up
                </button>
            </div>

            <!-- Auth Error / Success Message Box -->
            <div id="auth-alert" class="hidden mx-6 mt-4 p-3 rounded-xl text-xs font-medium flex items-center gap-2"></div>

            <!-- LOGIN FORM -->
            <form id="login-form" onsubmit="handleLogin(event)" class="p-6 space-y-4">
                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Email or Username</label>
                    <div class="relative">
                        <i data-lucide="user" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <input type="text" id="login-identifier" required placeholder="e.g., dispatcher@nasugbu.ph" class="w-full pl-10 pr-3 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 outline-none transition">
                    </div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Password</label>
                    <div class="relative">
                        <i data-lucide="lock" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <input type="password" id="login-password" required placeholder="••••••••" class="w-full pl-10 pr-10 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 outline-none transition">
                        <button type="button" onclick="togglePasswordVisibility('login-password')" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                            <i data-lucide="eye" class="w-4 h-4"></i>
                        </button>
                    </div>
                </div>

                <button type="submit" class="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-md shadow-emerald-600/20 transition flex items-center justify-center gap-2 active:scale-[0.99]">
                    <i data-lucide="log-in" class="w-4 h-4"></i>
                    <span>Access Warehouse Map</span>
                </button>

                <div class="relative flex py-1 items-center">
                    <div class="flex-grow border-t border-slate-200"></div>
                    <span class="flex-shrink mx-3 text-[11px] text-slate-400 font-medium">OR INSTANT PREVIEW</span>
                    <div class="flex-grow border-t border-slate-200"></div>
                </div>

                <button type="button" onclick="quickDemoLogin()" class="w-full py-2.5 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs border border-slate-300 transition flex items-center justify-center gap-2">
                    <i data-lucide="zap" class="w-4 h-4 text-amber-500"></i>
                    <span>Quick Demo Login (Nasugbu Coordinator)</span>
                </button>
            </form>

            <!-- SIGN UP FORM -->
            <form id="signup-form" onsubmit="handleSignup(event)" class="hidden p-6 space-y-3.5">
                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Full Name</label>
                    <div class="relative">
                        <i data-lucide="badge-check" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <input type="text" id="signup-name" required placeholder="Juan Dela Cruz" class="w-full pl-10 pr-3 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 outline-none transition">
                    </div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Email or Username</label>
                    <div class="relative">
                        <i data-lucide="mail" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <input type="text" id="signup-identifier" required placeholder="juan@nasugbulogistics.ph" class="w-full pl-10 pr-3 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 outline-none transition">
                    </div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Logistics Role</label>
                    <div class="relative">
                        <i data-lucide="briefcase" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <select id="signup-role" class="w-full pl-10 pr-3 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 bg-white outline-none transition">
                            <option value="Logistics Coordinator">Logistics Coordinator</option>
                            <option value="Warehouse Manager">Warehouse Manager</option>
                            <option value="Fleet Dispatcher">Fleet Dispatcher</option>
                            <option value="Supply Chain Analyst">Supply Chain Analyst</option>
                        </select>
                    </div>
                </div>

                <div>
                    <label class="block text-xs font-semibold text-slate-700 mb-1">Create Password</label>
                    <div class="relative">
                        <i data-lucide="lock" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                        <input type="password" id="signup-password" required minlength="4" placeholder="At least 4 characters" class="w-full pl-10 pr-10 py-2.5 rounded-xl border border-slate-300 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 text-sm text-slate-900 outline-none transition">
                        <button type="button" onclick="togglePasswordVisibility('signup-password')" class="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                            <i data-lucide="eye" class="w-4 h-4"></i>
                        </button>
                    </div>
                </div>

                <button type="submit" class="w-full py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-md shadow-emerald-600/20 transition flex items-center justify-center gap-2 mt-2 active:scale-[0.99]">
                    <i data-lucide="user-plus" class="w-4 h-4"></i>
                    <span>Create Account & Open Map</span>
                </button>
            </form>
        </div>
    </div>

    <!-- ============================================================================ -->
    <!-- 1. TOP APP BAR (WITH USER PROFILE & LOGOUT)                                  -->
    <!-- ============================================================================ -->
    <header class="bg-slate-900 text-white border-b border-slate-800 px-3 sm:px-5 py-2.5 flex items-center justify-between shrink-0 z-20 shadow-md">
        <div class="flex items-center gap-3">
            <div class="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-emerald-500 text-slate-950 flex items-center justify-center shadow-sm shrink-0">
                <i data-lucide="map-pin" class="w-5 h-5"></i>
            </div>
            <div>
                <h1 class="text-xs sm:text-base font-extrabold tracking-wide text-white leading-tight">
                    NASUGBU WAREHOUSE LOCATOR
                </h1>
                <p class="text-[11px] text-emerald-400 font-medium flex items-center gap-1.5">
                    <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                    <span id="hub-counter">Nasugbu, Batangas Grid (5 hubs)</span>
                </p>
            </div>
        </div>

        <div class="flex items-center gap-2">
            <!-- Clear Delivery Pin Button -->
            <button id="clear-pin-btn" onclick="clearDropoffPin()" class="hidden px-2.5 py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 border border-amber-400/40 text-amber-300 text-xs font-semibold transition flex items-center gap-1.5">
                <i data-lucide="pin-off" class="w-3.5 h-3.5"></i>
                <span class="hidden md:inline">Clear Pin</span>
            </button>

            <!-- Reset Map View Button -->
            <button onclick="resetMapView()" title="Reset View to All Nasugbu Hubs" class="px-2.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-100 text-xs font-semibold transition flex items-center gap-1.5 active:scale-95">
                <i data-lucide="locate-fixed" class="w-3.5 h-3.5 text-emerald-400"></i>
                <span class="hidden sm:inline">Fit All Hubs</span>
            </button>

            <!-- Logged-In User Badge & Logout -->
            <div class="flex items-center gap-2 pl-2 border-l border-slate-700">
                <div class="hidden md:flex flex-col text-right">
                    <span id="user-display-name" class="text-xs font-bold text-white leading-none">Guest User</span>
                    <span id="user-display-role" class="text-[10px] text-emerald-400 font-medium mt-0.5">Coordinator</span>
                </div>
                <button onclick="handleLogout()" title="Log Out" class="px-2.5 py-1.5 rounded-xl bg-rose-500/15 hover:bg-rose-500/25 border border-rose-500/30 text-rose-300 text-xs font-semibold transition flex items-center gap-1.5">
                    <i data-lucide="log-out" class="w-3.5 h-3.5"></i>
                    <span class="hidden sm:inline">Logout</span>
                </button>
            </div>
        </div>
    </header>

    <!-- ============================================================================ -->
    <!-- MAIN CONTENT WORKSPACE (HIGH-CLARITY MAP + DIRECTORY SIDEBAR)                -->
    <!-- ============================================================================ -->
    <main class="flex-1 flex flex-col lg:flex-row overflow-hidden relative">

        <!-- MAP SECTION (Top on Mobile, Right/Main on Desktop) -->
        <div class="relative w-full h-[54%] lg:h-full lg:flex-1 order-1 lg:order-2">
            <div id="map" class="w-full h-full z-10"></div>

            <!-- HIGH-CLARITY MAP LAYER SWITCHER (Top-Right of Map) -->
            <div class="absolute top-3 right-3 z-20 flex flex-col items-end gap-2">
                <div class="bg-white/95 backdrop-blur-md border border-slate-300 rounded-xl p-1 shadow-lg flex items-center gap-1">
                    <button onclick="switchMapLayer('osm')" id="layer-btn-osm" class="map-layer-btn px-2.5 py-1.5 rounded-lg text-[11px] font-bold bg-emerald-600 text-white shadow-sm transition flex items-center gap-1">
                        <i data-lucide="map" class="w-3.5 h-3.5"></i>
                        <span>Full Places &amp; Streets</span>
                    </button>
                    <button onclick="switchMapLayer('satellite')" id="layer-btn-satellite" class="map-layer-btn px-2.5 py-1.5 rounded-lg text-[11px] font-semibold text-slate-700 hover:bg-slate-100 transition flex items-center gap-1">
                        <i data-lucide="satellite" class="w-3.5 h-3.5"></i>
                        <span>Satellite + Labels</span>
                    </button>
                    <button onclick="switchMapLayer('topo')" id="layer-btn-topo" class="map-layer-btn px-2.5 py-1.5 rounded-lg text-[11px] font-semibold text-slate-700 hover:bg-slate-100 transition flex items-center gap-1">
                        <i data-lucide="mountain" class="w-3.5 h-3.5"></i>
                        <span>Topo &amp; Terrain</span>
                    </button>
                </div>

                <!-- Toggle Barangay Landmarks Pill -->
                <button onclick="toggleLandmarks()" id="landmark-toggle-btn" class="bg-white/95 backdrop-blur-md border border-slate-300 rounded-xl px-3 py-1.5 shadow-md text-[11px] font-semibold text-slate-800 hover:bg-slate-50 transition flex items-center gap-1.5">
                    <i data-lucide="eye" class="w-3.5 h-3.5 text-emerald-600"></i>
                    <span>Barangay Labels: ON</span>
                </button>
            </div>

            <!-- Interactive Map Hint Pill (Bottom-Left) -->
            <div class="hidden sm:block absolute bottom-6 left-3 z-20 pointer-events-none">
                <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/95 backdrop-blur-md border border-slate-300 text-[11px] text-slate-800 shadow-lg font-medium">
                    <i data-lucide="mouse-pointer-click" class="w-3.5 h-3.5 text-amber-600 shrink-0"></i>
                    <span>Click any place in Nasugbu to drop a <strong>Delivery Pin</strong> & check distance</span>
                </div>
            </div>

            <!-- Map Legend Overlay (Bottom-Right) -->
            <div class="absolute bottom-6 right-3 z-20 bg-white/95 backdrop-blur-md border border-slate-300 rounded-xl p-2.5 sm:p-3 shadow-xl">
                <div class="text-[10px] font-extrabold uppercase tracking-wider text-slate-500 mb-1.5">Hub Legend</div>
                <div class="space-y-1 text-[11px] font-semibold text-slate-800">
                    <div class="flex items-center gap-2">
                        <span class="w-3 h-3 rounded-full bg-[#059669] ring-2 ring-emerald-200"></span>
                        <span>Central Hub</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="w-3 h-3 rounded-full bg-[#2563EB] ring-2 ring-blue-200"></span>
                        <span>Fulfillment Center</span>
                    </div>
                    <div class="flex items-center gap-2">
                        <span class="w-3 h-3 rounded-full bg-[#0891B2] ring-2 ring-cyan-200"></span>
                        <span>Cold Storage</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- SIDEBAR / DIRECTORY PANEL (Bottom on Mobile, Left on Desktop) -->
        <aside class="w-full h-[46%] lg:h-full lg:w-[430px] xl:w-[460px] bg-slate-50 border-t lg:border-t-0 lg:border-r border-slate-200 flex flex-col order-2 lg:order-1 z-20">
            
            <!-- SEARCH & TYPE FILTER BAR -->
            <div class="p-3 border-b border-slate-200 space-y-2 bg-white shrink-0">
                <div class="relative">
                    <i data-lucide="search" class="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2"></i>
                    <input 
                        type="text" 
                        id="search-input" 
                        placeholder="Search warehouse, barangay (Wawa, Aga...), ID..." 
                        class="w-full bg-slate-100 border border-slate-300 focus:bg-white focus:border-emerald-600 focus:ring-2 focus:ring-emerald-500/20 rounded-xl pl-10 pr-9 py-2 text-xs sm:text-sm text-slate-900 placeholder-slate-500 outline-none transition"
                    >
                    <button id="clear-search-btn" onclick="clearSearch()" class="hidden absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700">
                        <i data-lucide="x" class="w-4 h-4"></i>
                    </button>
                </div>

                <!-- Filter Chips -->
                <div class="flex items-center gap-1.5 overflow-x-auto pb-0.5" id="filter-chips">
                    <button onclick="setTypeFilter('ALL')" data-type="ALL" class="filter-chip active px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-900 text-white transition shrink-0">
                        All Hubs (5)
                    </button>
                    <button onclick="setTypeFilter('Central Hub')" data-type="Central Hub" class="filter-chip px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-200/80 text-slate-700 hover:bg-slate-300/70 transition shrink-0">
                        Central Hub
                    </button>
                    <button onclick="setTypeFilter('Fulfillment Center')" data-type="Fulfillment Center" class="filter-chip px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-200/80 text-slate-700 hover:bg-slate-300/70 transition shrink-0">
                        Fulfillment
                    </button>
                    <button onclick="setTypeFilter('Cold Storage')" data-type="Cold Storage" class="filter-chip px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-200/80 text-slate-700 hover:bg-slate-300/70 transition shrink-0">
                        Cold Storage
                    </button>
                </div>
            </div>

            <!-- DELIVERY DROP-OFF PIN COVERAGE RESULTS (Dynamic Banner) -->
            <div id="dropoff-banner" class="hidden mx-3 mt-2.5 p-3 rounded-xl bg-amber-50 border border-amber-300 shrink-0 shadow-sm">
                <div class="flex items-start justify-between gap-2 mb-1">
                    <div class="flex items-center gap-1.5 text-amber-800 font-bold text-xs">
                        <i data-lucide="navigation" class="w-3.5 h-3.5 text-amber-600"></i>
                        <span>Delivery Drop-off Pin Analysis</span>
                    </div>
                    <button onclick="clearDropoffPin()" class="text-amber-700 hover:text-amber-950">
                        <i data-lucide="x" class="w-3.5 h-3.5"></i>
                    </button>
                </div>
                <p id="dropoff-coords" class="text-[11px] text-amber-900/80 font-mono mb-1.5"></p>
                <div id="dropoff-distances" class="space-y-1 max-h-24 overflow-y-auto pr-1 text-[11px]"></div>
            </div>

            <!-- SELECTED WAREHOUSE DETAIL BANNER -->
            <div id="selected-banner" class="hidden mx-3 mt-2.5 p-3.5 rounded-xl bg-white border-2 border-emerald-500 shadow-md shrink-0 transition-all">
                <div class="flex items-start justify-between gap-2">
                    <div>
                        <div class="flex items-center gap-2">
                            <span id="sel-id" class="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-100 text-emerald-800">NSG-01</span>
                            <span id="sel-barangay" class="text-xs font-semibold text-slate-500">Brgy. Poblacion</span>
                        </div>
                        <h2 id="sel-name" class="text-sm font-extrabold text-slate-900 mt-1">Nasugbu Central Logistics Hub</h2>
                        <p id="sel-specialty" class="text-xs text-emerald-700 font-semibold mt-0.5">Main Cross-Docking & Retail Goods</p>
                    </div>
                    <button onclick="deselectWarehouse()" class="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition">
                        <i data-lucide="x" class="w-4 h-4"></i>
                    </button>
                </div>

                <div class="grid grid-cols-2 gap-2 mt-2.5 pt-2 border-t border-slate-100 text-[11px]">
                    <div>
                        <span class="text-slate-400 font-medium block">Manager & Hotline</span>
                        <span id="sel-manager" class="text-slate-800 font-bold">Engr. Mateo Mendoza</span>
                        <span id="sel-hotline" class="text-slate-600 block">(043) 416-2201</span>
                    </div>
                    <div>
                        <span class="text-slate-400 font-medium block">Coordinates</span>
                        <span id="sel-coords" class="text-slate-800 font-mono font-semibold">14.0718, 120.6322</span>
                        <span id="sel-town-dist" class="text-slate-600 block mt-0.5">Town Plaza: 0.09 km</span>
                    </div>
                </div>

                <div class="mt-2.5 flex items-center justify-between gap-2">
                    <span id="sel-radius-badge" class="text-[11px] text-slate-700 bg-slate-100 px-2.5 py-1.5 rounded-lg border border-slate-200 font-medium">
                        Coverage: <strong>4.5 km</strong>
                    </span>
                    <button id="dispatch-btn" onclick="triggerDispatchAlert()" class="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs py-2 px-3 rounded-lg transition flex items-center justify-center gap-1.5 shadow-sm active:scale-95">
                        <i data-lucide="send" class="w-3.5 h-3.5"></i>
                        <span>Dispatch Alert</span>
                    </button>
                </div>
            </div>

            <!-- WAREHOUSE DIRECTORY LIST -->
            <div class="flex-1 overflow-y-auto p-3 space-y-2" id="warehouse-list">
                <!-- Cards injected via JS -->
            </div>
        </aside>
    </main>

    <!-- TOAST NOTIFICATION CONTAINER -->
    <div id="toast-container" class="fixed bottom-5 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 pointer-events-none w-[90%] max-w-md"></div>

    <script>
        // ============================================================================
        // 0. AUTHENTICATION SYSTEM (SIGN UP, LOGIN, DEMO ACCESS, SESSION PERSISTENCE)
        // ============================================================================

        const USERS_STORAGE_KEY = 'nasugbu_locator_users_v1';
        const SESSION_STORAGE_KEY = 'nasugbu_locator_session_v1';

        function getStoredUsers() {
            try {
                return JSON.parse(localStorage.getItem(USERS_STORAGE_KEY)) || [];
            } catch {
                return [];
            }
        }

        function saveStoredUsers(users) {
            localStorage.setItem(USERS_STORAGE_KEY, JSON.stringify(users));
        }

        function switchAuthTab(mode) {
            const loginForm = document.getElementById('login-form');
            const signupForm = document.getElementById('signup-form');
            const tabLogin = document.getElementById('tab-login');
            const tabSignup = document.getElementById('tab-signup');
            const alertBox = document.getElementById('auth-alert');

            alertBox.classList.add('hidden');

            if (mode === 'login') {
                loginForm.classList.remove('hidden');
                signupForm.classList.add('hidden');
                tabLogin.className = "py-3 text-xs sm:text-sm font-bold text-emerald-700 border-b-2 border-emerald-600 bg-white transition";
                tabSignup.className = "py-3 text-xs sm:text-sm font-semibold text-slate-500 hover:text-slate-800 border-b-2 border-transparent transition";
            } else {
                loginForm.classList.add('hidden');
                signupForm.classList.remove('hidden');
                tabSignup.className = "py-3 text-xs sm:text-sm font-bold text-emerald-700 border-b-2 border-emerald-600 bg-white transition";
                tabLogin.className = "py-3 text-xs sm:text-sm font-semibold text-slate-500 hover:text-slate-800 border-b-2 border-transparent transition";
            }
        }

        function showAuthAlert(message, isError = true) {
            const alertBox = document.getElementById('auth-alert');
            alertBox.classList.remove('hidden');
            alertBox.className = isError
                ? "mx-6 mt-4 p-3 rounded-xl text-xs font-semibold flex items-center gap-2 bg-rose-50 text-rose-700 border border-rose-200"
                : "mx-6 mt-4 p-3 rounded-xl text-xs font-semibold flex items-center gap-2 bg-emerald-50 text-emerald-700 border border-emerald-200";
            alertBox.innerHTML = <i data-lucide="${isError ? 'alert-circle' : 'check-circle-2'}" class="w-4 h-4 shrink-0"></i><span>${message}</span>;
            lucide.createIcons();
        }

        function togglePasswordVisibility(inputId) {
            const input = document.getElementById(inputId);
            input.type = input.type === 'password' ? 'text' : 'password';
        }

        function handleSignup(e) {
            e.preventDefault();
            const fullName = document.getElementById('signup-name').value.trim();
            const identifier = document.getElementById('signup-identifier').value.trim().toLowerCase();
            const role = document.getElementById('signup-role').value;
            const password = document.getElementById('signup-password').value;

            if (!fullName || !identifier || !password) {
                showAuthAlert("Please complete all required fields.");
                return;
            }

            const users = getStoredUsers();
            if (users.some(u => u.identifier === identifier)) {
                showAuthAlert("An account with this email or username already exists. Please log in.");
                return;
            }

            const newUser = { fullName, identifier, role, password };
            users.push(newUser);
            saveStoredUsers(users);

            completeLoginSession(newUser);
            showToast("Account Created", Welcome to Nasugbu Grid, ${fullName}!);
        }

        function handleLogin(e) {
            e.preventDefault();
            const identifier = document.getElementById('login-identifier').value.trim().toLowerCase();
            const password = document.getElementById('login-password').value;

            const users = getStoredUsers();
            const matchedUser = users.find(u => u.identifier === identifier && u.password === password);

            if (!matchedUser) {
                showAuthAlert("Invalid email/username or password. Try signing up or use Quick Demo Login.");
                return;
            }

            completeLoginSession(matchedUser);
            showToast("Logged In", Welcome back, ${matchedUser.fullName}!);
        }

        function quickDemoLogin() {
            const demoUser = {
                fullName: "Engr. Mateo Mendoza",
                identifier: "demo@nasugbu.ph",
                role: "Nasugbu Logistics Coordinator"
            };
            completeLoginSession(demoUser);
            showToast("Demo Access Active", "Signed in as Nasugbu Logistics Coordinator");
        }

        function completeLoginSession(user) {
            localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(user));
            document.getElementById('user-display-name').textContent = user.fullName;
            document.getElementById('user-display-role').textContent = user.role;
            document.getElementById('auth-screen').classList.add('hidden');

            // Ensure Leaflet recalculates container size properly after overlay hides
            setTimeout(() => {
                map.invalidateSize();
                map.setView([14.0850, 120.6550], 13);
            }, 150);
        }

        function handleLogout() {
            localStorage.removeItem(SESSION_STORAGE_KEY);
            document.getElementById('login-password').value = "";
            document.getElementById('auth-screen').classList.remove('hidden');
            switchAuthTab('login');
        }

        function checkExistingSession() {
            try {
                const saved = JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY));
                if (saved && saved.fullName) {
                    document.getElementById('user-display-name').textContent = saved.fullName;
                    document.getElementById('user-display-role').textContent = saved.role || "Logistics Coordinator";
                    document.getElementById('auth-screen').classList.add('hidden');
                }
            } catch {
                // Stay on auth screen
            }
        }

        // ============================================================================
        // 1. WAREHOUSE DOMAIN CLASSES & DATA (NASUGBU, BATANGAS)
        // ============================================================================

        const WarehouseType = {
            CENTRAL: { label: "Central Hub", color: "#059669", icon: "warehouse" },
            FULFILLMENT: { label: "Fulfillment Center", color: "#2563EB", icon: "truck" },
            COLD_STORAGE: { label: "Cold Storage", color: "#0891B2", icon: "snowflake" }
        };

        class BaseWarehouse {
            constructor(id, name, barangay, latitude, longitude, coverageRadiusKm, type, manager, hotline, specialty) {
                this.id = id;
                this.name = name;
                this.barangay = barangay;
                this.latitude = latitude;
                this.longitude = longitude;
                this.coverageRadiusKm = coverageRadiusKm;
                this.type = type;
                this.manager = manager;
                this.hotline = hotline;
                this.specialty = specialty;
            }

            // Haversine Distance Calculation (in km)
            distanceFromKm(targetLat, targetLon) {
                const earthRadiusKm = 6371.0;
                const dLat = (targetLat - this.latitude) * Math.PI / 180;
                const dLon = (targetLon - this.longitude) * Math.PI / 180;
                const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                          Math.cos(this.latitude * Math.PI / 180) * Math.cos(targetLat * Math.PI / 180) *
                          Math.sin(dLon / 2) * Math.sin(dLon / 2);
                const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return earthRadiusKm * c;
            }

            isWithinCoverage(targetLat, targetLon) {
                return this.distanceFromKm(targetLat, targetLon) <= this.coverageRadiusKm;
            }
        }

        const warehouses = [
            new BaseWarehouse(
                "NSG-01",
                "Nasugbu Central Logistics Hub",
                "Brgy. Poblacion",
                14.0718,
                120.6322,
                4.5,
                WarehouseType.CENTRAL,
                "Engr. Mateo Mendoza",
                "(043) 416-2201",
                "Main Cross-Docking & Retail Goods"
            ),
            new BaseWarehouse(
                "NSG-02",
                "Wawa Port Cold Chain Facility",
                "Brgy. Wawa",
                14.0842,
                120.6195,
                3.5,
                WarehouseType.COLD_STORAGE,
                "Clarissa Dimaano",
                "(043) 416-3109",
                "Frozen Seafood & Perishable Storage"
            ),
            new BaseWarehouse(
                "NSG-03",
                "Natipuan Coastal Resort Supply Hub",
                "Brgy. Natipuan",
                14.1325,
                120.6148,
                4.0,
                WarehouseType.FULFILLMENT,
                "Jomar Villalobos",
                "(043) 416-8840",
                "Hospitality, Hotel & Resort Supplies"
            ),
            new BaseWarehouse(
                "NSG-04",
                "Aga Highland Agro-Distribution Center",
                "Brgy. Aga",
                14.1012,
                120.7285,
                5.0,
                WarehouseType.CENTRAL,
                "Ramonito Laurel",
                "(043) 416-5512",
                "Agricultural Produce & Tagaytay Route"
            ),
            new BaseWarehouse(
                "NSG-05",
                "Lumbangan South Border Depot",
                "Brgy. Lumbangan",
                14.0345,
                120.6480,
                3.8,
                WarehouseType.FULFILLMENT,
                "Liza Atienza",
                "(043) 416-7715",
                "Bulk Construction & South Batangas Dispatch"
            )
        ];

        // Notable Barangays & Landmarks in Nasugbu for crystal-clear geographic context
        const nasugbuLandmarks = [
            { name: "Brgy. Poblacion / Town Plaza", lat: 14.0728, lon: 120.6315 },
            { name: "Brgy. Bucana / Nasugbu Beach", lat: 14.0665, lon: 120.6238 },
            { name: "Brgy. Wawa Port & Pier", lat: 14.0858, lon: 120.6168 },
            { name: "Brgy. Natipuan / Terrazas", lat: 14.1365, lon: 120.6120 },
            { name: "Brgy. Papaya / Cove", lat: 14.1510, lon: 120.6065 },
            { name: "Brgy. Calayo", lat: 14.1620, lon: 120.6125 },
            { name: "Brgy. Looc", lat: 14.1685, lon: 120.6350 },
            { name: "Brgy. Kaylaway (Tagaytay-Nasugbu Hwy)", lat: 14.0935, lon: 120.7680 },
            { name: "Brgy. Banilad Junction", lat: 14.0785, lon: 120.6780 },
            { name: "Brgy. Pantalan / Central Area", lat: 14.0545, lon: 120.6410 }
        ];

        const TOWN_PLAZA = { lat: 14.0725, lon: 120.6318 };

        // App State
        let selectedWarehouseId = null;
        let searchQuery = "";
        let activeTypeFilter = "ALL";
        let dropoffPinMarker = null;
        let dropoffLines = [];
        let showLandmarks = true;

        const markersMap = {};
        const circlesMap = {};
        const landmarkMarkers = [];

        // ============================================================================
        // 2. INITIALIZE ULTRA-CLEAR MULTI-LAYER LEAFLET MAP (100% NO API KEY)
        // ============================================================================

        const map = L.map('map', {
            center: [14.0850, 120.6550],
            zoom: 13,
            zoomControl: true
        });

        // Layer 1 (DEFAULT): Full OpenStreetMap Standard (100% Free, No API Key - shows every street, barangay, school, church, resort & river)
        const fullOsmPlacesLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors',
            maxZoom: 19
        });

        // Layer 2: Esri World Imagery + Esri Transportation Roads + Esri Boundaries & Places (100% Free, No API Key)
        const satelliteBase = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            attribution: 'Tiles &copy; Esri',
            maxZoom: 19
        });
        const satelliteRoadsOverlay = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19
        });
        const satellitePlacesOverlay = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19
        });
        const satelliteHybridGroup = L.layerGroup([satelliteBase, satelliteRoadsOverlay, satellitePlacesOverlay]);

        // Layer 3: OpenTopoMap (100% Free, No API Key - topographic contours, roads & place names)
        const topoTerrainLayer = L.tileLayer('https://tile.opentopomap.org/{z}/{x}/{y}.png', {
            attribution: 'Map data: &copy; OpenStreetMap contributors, SRTM | Map style: &copy; OpenTopoMap',
            maxZoom: 17
        });

        // Set default active layer to Full OpenStreetMap Places & Streets
        let currentBaseLayer = fullOsmPlacesLayer;
        currentBaseLayer.addTo(map);

        function switchMapLayer(mode) {
            map.removeLayer(currentBaseLayer);

            document.querySelectorAll('.map-layer-btn').forEach(btn => {
                btn.className = "map-layer-btn px-2.5 py-1.5 rounded-lg text-[11px] font-semibold text-slate-700 hover:bg-slate-100 transition flex items-center gap-1";
            });

            const activeBtn = document.getElementById(layer-btn-${mode});
            activeBtn.className = "map-layer-btn px-2.5 py-1.5 rounded-lg text-[11px] font-bold bg-emerald-600 text-white shadow-sm transition flex items-center gap-1";

            if (mode === 'osm') {
                currentBaseLayer = fullOsmPlacesLayer;
            } else if (mode === 'satellite') {
                currentBaseLayer = satelliteHybridGroup;
            } else {
                currentBaseLayer = topoTerrainLayer;
            }

            currentBaseLayer.addTo(map);
        }

        // Render Barangay & Landmark Labels so all places in Nasugbu are clearly visible
        function renderLandmarks() {
            landmarkMarkers.forEach(m => map.removeLayer(m));
            landmarkMarkers.length = 0;

            if (!showLandmarks) return;

            nasugbuLandmarks.forEach(place => {
                const dotIcon = L.divIcon({
                    className: 'landmark-dot',
                    html: <div style="width: 8px; height: 8px; background: #475569; border: 2px solid #ffffff; border-radius: 50%; box-shadow: 0 1px 4px rgba(0,0,0,0.4);"></div>,
                    iconSize: [8, 8],
                    iconAnchor: [4, 4]
                });

                const marker = L.marker([place.lat, place.lon], {
                    icon: dotIcon,
                    interactive: false
                }).addTo(map);

                marker.bindTooltip(place.name, {
                    permanent: true,
                    direction: 'top',
                    offset: [0, -6],
                    className: 'landmark-label'
                });

                landmarkMarkers.push(marker);
            });
        }

        function toggleLandmarks() {
            showLandmarks = !showLandmarks;
            const btn = document.getElementById('landmark-toggle-btn');
            btn.innerHTML = showLandmarks
                ? <i data-lucide="eye" class="w-3.5 h-3.5 text-emerald-600"></i><span>Barangay Labels: ON</span>
                : <i data-lucide="eye-off" class="w-3.5 h-3.5 text-slate-400"></i><span>Barangay Labels: OFF</span>;
            lucide.createIcons();
            renderLandmarks();
        }

        function createCustomMarkerIcon(color, isSelected) {
            const size = isSelected ? 36 : 28;
            return L.divIcon({
                className: 'custom-warehouse-marker',
                html: `
                    <div style="position: relative; width: ${size}px; height: ${size}px; display: flex; align-items: center; justify-content: center;">
                        ${isSelected ? <div class="marker-pulse" style="position: absolute; width: 100%; height: 100%; border-radius: 50%; background: ${color};"></div> : ''}
                        <div style="
                            width: ${size - 6}px;
                            height: ${size - 6}px;
                            background-color: ${color};
                            border: 3px solid #ffffff;
                            border-radius: 50%;
                            box-shadow: 0 3px 10px rgba(15, 23, 42, 0.45);
                            z-index: 2;
                        "></div>
                    </div>
                `,
                iconSize: [size, size],
                iconAnchor: [size / 2, size / 2],
                popupAnchor: [0, -size / 2]
            });
        }

        // ============================================================================
        // 3. RENDER WAREHOUSES & DIRECTORY LIST
        // ============================================================================

        function getFilteredWarehouses() {
            return warehouses.filter(w => {
                const matchesType = activeTypeFilter === "ALL" || w.type.label === activeTypeFilter;
                const q = searchQuery.trim().toLowerCase();
                const matchesSearch = !q ||
                    w.name.toLowerCase().includes(q) ||
                    w.barangay.toLowerCase().includes(q) ||
                    w.id.toLowerCase().includes(q) ||
                    w.type.label.toLowerCase().includes(q);
                return matchesType && matchesSearch;
            });
        }

        function renderApp() {
            const filtered = getFilteredWarehouses();
            document.getElementById('hub-counter').textContent = Nasugbu, Batangas Grid (${filtered.length} hub${filtered.length === 1 ? '' : 's'});

            Object.values(markersMap).forEach(m => map.removeLayer(m));
            Object.values(circlesMap).forEach(c => map.removeLayer(c));

            filtered.forEach(w => {
                const isSelected = w.id === selectedWarehouseId;

                // Coverage Circle
                const circle = L.circle([w.latitude, w.longitude], {
                    radius: w.coverageRadiusKm * 1000,
                    color: w.type.color,
                    weight: isSelected ? 3 : 1.8,
                    opacity: isSelected ? 0.95 : 0.7,
                    fillColor: w.type.color,
                    fillOpacity: isSelected ? 0.22 : 0.10
                }).addTo(map);
                circlesMap[w.id] = circle;

                // Marker with Permanent Readable Label
                const marker = L.marker([w.latitude, w.longitude], {
                    icon: createCustomMarkerIcon(w.type.color, isSelected),
                    zIndexOffset: isSelected ? 1000 : 200
                }).addTo(map);

                marker.bindTooltip(${w.id} • ${w.barangay}, {
                    permanent: true,
                    direction: 'bottom',
                    offset: [0, 12],
                    className: 'warehouse-permanent-label'
                });

                marker.bindPopup(`
                    <div style="min-width: 205px; padding: 2px;">
                        <div style="font-size: 10px; font-weight: 800; color: ${w.type.color}; text-transform: uppercase;">
                            ${w.id} • ${w.type.label}
                        </div>
                        <div style="font-size: 13px; font-weight: 800; color: #0f172a; margin: 2px 0;">
                            ${w.name}
                        </div>
                        <div style="font-size: 11px; color: #475569; font-weight: 600;">
                            ${w.barangay} • ${w.coverageRadiusKm} km radius
                        </div>
                        <div style="font-size: 11px; color: #334155; margin-top: 5px; border-top: 1px solid #e2e8f0; padding-top: 5px;">
                            ${w.specialty}
                        </div>
                    </div>
                `);

                marker.on('click', () => {
                    selectWarehouse(w.id, false);
                });

                markersMap[w.id] = marker;

                if (isSelected) {
                    marker.openPopup();
                }
            });

            // Render Sidebar Directory Cards
            const listContainer = document.getElementById('warehouse-list');
            if (filtered.length === 0) {
                listContainer.innerHTML = `
                    <div class="text-center py-10 px-4">
                        <p class="text-sm text-slate-600 font-semibold">No warehouses match your search.</p>
                        <button onclick="clearSearch(); setTypeFilter('ALL');" class="mt-2 text-xs text-emerald-700 font-bold hover:underline">Reset filters</button>
                    </div>
                `;
            } else {
                listContainer.innerHTML = filtered.map(w => {
                    const isSelected = w.id === selectedWarehouseId;
                    return `
                        <div onclick="selectWarehouse('${w.id}', true)" 
                             class="cursor-pointer rounded-xl p-3 border transition-all duration-200 ${
                                isSelected 
                                ? 'bg-emerald-50/70 border-emerald-500 shadow-md' 
                                : 'bg-white border-slate-200 hover:border-slate-300 hover:shadow-sm'
                             }">
                            <div class="flex items-center gap-3">
                                <div class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0" 
                                     style="background-color: ${w.type.color}18; color: ${w.type.color}; border: 1px solid ${w.type.color}35;">
                                    <i data-lucide="${w.type.icon}" class="w-5 h-5"></i>
                                </div>
                                <div class="flex-1 min-w-0">
                                    <div class="flex items-center justify-between gap-2">
                                        <div class="flex items-center gap-1.5 truncate">
                                            <span class="text-[11px] font-extrabold" style="color: ${w.type.color};">${w.id}</span>
                                            <span class="text-slate-300">•</span>
                                            <span class="text-xs font-semibold text-slate-500 truncate">${w.barangay}</span>
                                        </div>
                                        <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200 shrink-0">
                                            ${w.coverageRadiusKm} km
                                        </span>
                                    </div>
                                    <h3 class="text-sm font-bold text-slate-900 truncate mt-0.5">${w.name}</h3>
                                    <p class="text-[11px] text-slate-500 truncate mt-0.5">${w.type.label} • ${w.specialty}</p>
                                </div>
                                <i data-lucide="chevron-right" class="w-4 h-4 text-slate-400 shrink-0"></i>
                            </div>
                        </div>
                    `;
                }).join('');
            }

            lucide.createIcons();
            updateSelectedBanner();
        }

        // ============================================================================
        // 4. SELECTION, SEARCH & FILTER HANDLERS
        // ============================================================================

        function selectWarehouse(id, flyTo = true) {
            selectedWarehouseId = id;
            const warehouse = warehouses.find(w => w.id === id);
            if (warehouse && flyTo) {
                map.flyTo([warehouse.latitude, warehouse.longitude], 14.5, {
                    duration: 0.85
                });
            }
            renderApp();
        }

        function deselectWarehouse() {
            selectedWarehouseId = null;
            map.closePopup();
            renderApp();
        }

        function updateSelectedBanner() {
            const banner = document.getElementById('selected-banner');
            const warehouse = warehouses.find(w => w.id === selectedWarehouseId);

            if (!warehouse) {
                banner.classList.add('hidden');
                return;
            }

            banner.classList.remove('hidden');
            const idBadge = document.getElementById('sel-id');
            idBadge.textContent = warehouse.id;
            idBadge.style.backgroundColor = ${warehouse.type.color}20;
            idBadge.style.color = warehouse.type.color;

            document.getElementById('sel-barangay').textContent = warehouse.barangay;
            document.getElementById('sel-name').textContent = warehouse.name;
            document.getElementById('sel-specialty').textContent = warehouse.specialty;
            document.getElementById('sel-specialty').style.color = warehouse.type.color;
            document.getElementById('sel-manager').textContent = warehouse.manager;
            document.getElementById('sel-hotline').textContent = warehouse.hotline;
            document.getElementById('sel-coords').textContent = ${warehouse.latitude.toFixed(4)}, ${warehouse.longitude.toFixed(4)};

            const distTown = warehouse.distanceFromKm(TOWN_PLAZA.lat, TOWN_PLAZA.lon);
            const isCovered = warehouse.isWithinCoverage(TOWN_PLAZA.lat, TOWN_PLAZA.lon);
            document.getElementById('sel-town-dist').innerHTML = `
                Town Plaza: <strong>${distTown.toFixed(2)} km</strong> 
                <span class="font-bold ${isCovered ? 'text-emerald-600' : 'text-rose-600'}">(${isCovered ? 'Covered' : 'Outside'})</span>
            `;

            document.getElementById('sel-radius-badge').innerHTML = Coverage: <strong>${warehouse.coverageRadiusKm} km</strong>;
        }

        function resetMapView() {
            selectedWarehouseId = null;
            map.closePopup();
            const filtered = getFilteredWarehouses();
            if (filtered.length > 0) {
                const bounds = L.latLngBounds(filtered.map(w => [w.latitude, w.longitude]));
                map.flyToBounds(bounds, { padding: [60, 60], maxZoom: 13, duration: 0.85 });
            } else {
                map.flyTo([14.0850, 120.6550], 13);
            }
            renderApp();
        }

        const searchInput = document.getElementById('search-input');
        const clearSearchBtn = document.getElementById('clear-search-btn');

        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value;
            clearSearchBtn.classList.toggle('hidden', searchQuery.length === 0);
            renderApp();
        });

        function clearSearch() {
            searchQuery = "";
            searchInput.value = "";
            clearSearchBtn.classList.add('hidden');
            renderApp();
        }

        function setTypeFilter(typeLabel) {
            activeTypeFilter = typeLabel;
            document.querySelectorAll('.filter-chip').forEach(btn => {
                if (btn.dataset.type === typeLabel) {
                    btn.className = "filter-chip active px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-900 text-white transition shrink-0";
                } else {
                    btn.className = "filter-chip px-2.5 py-1 rounded-lg text-xs font-semibold bg-slate-200/80 text-slate-700 hover:bg-slate-300/70 transition shrink-0";
                }
            });
            renderApp();
        }

        // ============================================================================
        // 5. INTERACTIVE MAP CLICK: DELIVERY DROP-OFF PIN & HAVERSINE ANALYZER
        // ============================================================================

        map.on('click', function(e) {
            const { lat, lng } = e.latlng;
            placeDropoffPin(lat, lng);
        });

        function placeDropoffPin(lat, lng) {
            clearDropoffGraphics();

            const pinIcon = L.divIcon({
                className: 'dropoff-pin',
                html: `
                    <div style="display:flex; flex-direction:column; align-items:center;">
                        <div style="background:#d97706; color:#ffffff; font-weight:800; font-size:10px; padding:2px 7px; border-radius:6px; white-space:nowrap; box-shadow:0 3px 8px rgba(0,0,0,0.35); border:1.5px solid #fff; margin-bottom:2px;">
                            Delivery Drop-off
                        </div>
                        <div style="width:14px; height:14px; background:#f59e0b; border:3px solid #ffffff; border-radius:50%; box-shadow:0 2px 8px rgba(0,0,0,0.5);"></div>
                    </div>
                `,
                iconSize: [96, 38],
                iconAnchor: [48, 31]
            });

            dropoffPinMarker = L.marker([lat, lng], { icon: pinIcon, zIndexOffset: 2000 }).addTo(map);

            const results = warehouses.map(w => {
                const dist = w.distanceFromKm(lat, lng);
                const covered = w.isWithinCoverage(lat, lng);
                return { warehouse: w, dist, covered };
            }).sort((a, b) => a.dist - b.dist);

            results.forEach((res, idx) => {
                if (res.covered || idx === 0) {
                    const line = L.polyline(
                        [[res.warehouse.latitude, res.warehouse.longitude], [lat, lng]],
                        {
                            color: res.covered ? res.warehouse.type.color : '#64748b',
                            weight: res.covered ? 3 : 2,
                            dashArray: '6, 6',
                            opacity: 0.9
                        }
                    ).addTo(map);
                    dropoffLines.push(line);
                }
            });

            document.getElementById('clear-pin-btn').classList.remove('hidden');
            const banner = document.getElementById('dropoff-banner');
            banner.classList.remove('hidden');
            document.getElementById('dropoff-coords').textContent = Target Coords: ${lat.toFixed(5)}, ${lng.toFixed(5)};

            const listHtml = results.map(r => `
                <div onclick="selectWarehouse('${r.warehouse.id}', true)" class="cursor-pointer flex items-center justify-between py-1 px-2 rounded-lg bg-white hover:bg-amber-100/60 border border-amber-200/80">
                    <span class="text-slate-800 font-medium truncate pr-2">
                        <strong style="color:${r.warehouse.type.color}">${r.warehouse.id}</strong> ${r.warehouse.barangay}
                    </span>
                    <div class="flex items-center gap-1.5 shrink-0">
                        <span class="font-mono font-bold text-slate-700">${r.dist.toFixed(2)} km</span>
                        <span class="px-1.5 py-0.5 rounded text-[9px] font-extrabold ${
                            r.covered ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-200 text-slate-600'
                        }">
                            ${r.covered ? 'COVERED' : 'OUT'}
                        </span>
                    </div>
                </div>
            `).join('');

            document.getElementById('dropoff-distances').innerHTML = listHtml;
        }

        function clearDropoffGraphics() {
            if (dropoffPinMarker) {
                map.removeLayer(dropoffPinMarker);
                dropoffPinMarker = null;
            }
            dropoffLines.forEach(l => map.removeLayer(l));
            dropoffLines = [];
        }

        function clearDropoffPin() {
            clearDropoffGraphics();
            document.getElementById('dropoff-banner').classList.add('hidden');
            document.getElementById('clear-pin-btn').classList.add('hidden');
        }

        // ============================================================================
        // 6. TOAST NOTIFICATIONS
        // ============================================================================

        function showToast(title, subtitle) {
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            toast.className = "pointer-events-auto flex items-center gap-3 bg-slate-900 border border-emerald-500/50 text-white px-4 py-3 rounded-xl shadow-2xl transform transition-all duration-300 translate-y-2 opacity-0";
            toast.innerHTML = `
                <div class="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
                    <i data-lucide="radio" class="w-4 h-4"></i>
                </div>
                <div class="flex-1 min-w-0">
                    <div class="text-xs font-bold text-emerald-400">${title}</div>
                    <div class="text-[11px] text-slate-300 truncate">${subtitle}</div>
                </div>
            `;

            container.appendChild(toast);
            lucide.createIcons();

            requestAnimationFrame(() => {
                toast.classList.remove('translate-y-2', 'opacity-0');
            });

            setTimeout(() => {
                toast.classList.add('opacity-0', 'translate-y-2');
                setTimeout(() => toast.remove(), 300);
            }, 3500);
        }

        function triggerDispatchAlert() {
            const warehouse = warehouses.find(w => w.id === selectedWarehouseId);
            if (!warehouse) return;
            showToast(
                Dispatch Alert Sent • ${warehouse.id},
                Notified ${warehouse.manager} (${warehouse.hotline})
            );
        }

        // Initial Boot
        window.addEventListener('resize', () => {
            map.invalidateSize();
        });

        window.addEventListener('load', () => {
            lucide.createIcons();
            checkExistingSession();
            renderLandmarks();
            renderApp();
            map.invalidateSize();
            map.setView([14.0850, 120.6550], 13);
        });
    </script>
</body>
</html>
