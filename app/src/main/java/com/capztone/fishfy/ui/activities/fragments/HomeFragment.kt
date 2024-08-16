package com.capztone.fishfy.ui.activities.fragments


import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.capztone.fishfy.R
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import com.google.firebase.storage.FirebaseStorage
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.interfaces.ItemClickListener
import com.denzcoskun.imageslider.models.SlideModel
import com.capztone.fishfy.databinding.FragmentHomeBinding
import com.capztone.fishfy.ui.activities.LocationActivity
import com.capztone.fishfy.ui.activities.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import com.google.android.gms.location.FusedLocationProviderClient
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.fishfy.ui.activities.adapters.ExploreShopAdapter
import com.capztone.fishfy.ui.activities.adapters.HomeDiscountAdapter
import com.capztone.fishfy.ui.activities.adapters.NearItemAdapter
import com.capztone.fishfy.ui.activities.adapters.PreviousOrderAdapter
import com.capztone.fishfy.ui.activities.models.MenuItem
import com.capztone.fishfy.ui.activities.models.PreviousItem
import com.capztone.fishfy.ui.activities.ViewModel.HomeViewModel
import com.capztone.fishfy.ui.activities.adapters.CategoryAdapter
import com.capztone.fishfy.ui.activities.adapters.DealItemAdapter
import com.capztone.fishfy.ui.activities.models.CartItems
import com.capztone.fishfy.ui.activities.models.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import com.capztone.fishfy.ui.activities.LocationNotAvailable
import com.capztone.fishfy.ui.activities.Utils.NetworkReceiver
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson


class HomeFragment : Fragment(), ExploreShopAdapter.OnItemClickListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var databases: FirebaseDatabase
    private lateinit var viewModel: HomeViewModel
    private var quantity: Int = 0

    private lateinit var databaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var currentUserId: String
    private lateinit var networkReceiver: NetworkReceiver


    private val cartItems = mutableListOf<CartItems>()
    private val  discountItem = mutableListOf<DiscountItem>()
    private lateinit var userId: String
    private val previousItem = mutableListOf<PreviousItem>()
    private var menuItems = mutableListOf<MenuItem>()
    private lateinit var adapter: NearItemAdapter
    private lateinit var dealadapter: DealItemAdapter
    private lateinit var exploreShopAdapter: ExploreShopAdapter
    private lateinit var adapter1: PreviousOrderAdapter
    private lateinit var categories: MutableList<Category>
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var homeDiscountAdapter: HomeDiscountAdapter // Initialize HomeDiscountAdapter
    private lateinit var navController: NavController



    private val buyHistory: MutableList<PreviousItem> = mutableListOf()
    private lateinit var homeViewModel: HomeViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        adapter1 = PreviousOrderAdapter(buyHistory,requireContext()) // Initialize adapter1 here
        navController = findNavController()
        observeViewModel()

        database = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("Locations")
        databaseRef = FirebaseDatabase.getInstance().reference
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        // Add this part to check network connectivity
        // Initialize and register the network receiver
        networkReceiver = NetworkReceiver { isConnected ->
            handleNetworkStatus(isConnected) // This should be a Boolean
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, filter)

        // Check network status initially
        handleNetworkStatus(isNetworkAvailable())
        binding.btnRetry.setOnClickListener {
            if (isNetworkAvailable()) {

                findNavController().popBackStack() // Example action, modify as needed
            } else {
                // Show toast if network is still not available
                Toast.makeText(requireContext(), "Please check your network", Toast.LENGTH_SHORT).show()
            }
        }
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE

        binding.progress.setProgressVector(resources.getDrawable(R.drawable.spinload))
        binding.progress.setTextViewVisibility(true)
        binding.progress.setTextStyle(true)
        binding.progress.setTextColor(Color.YELLOW)
        binding.progress.setTextSize(12F)
        binding.progress.setTextMsg("Please Wait")
        binding.progress.setEnlarge(5)
        // Start a delay to hide the loading indicator after 5000 milliseconds (5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1500)


        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }

        databases = FirebaseDatabase.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        auth = FirebaseAuth.getInstance()


        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            adapter.updateData(items)
            updateTitlesVisibility(menuItems, cartItems, categories)
        }



        homeDiscountAdapter = HomeDiscountAdapter(requireContext()) // Initialize HomeDiscountAdapter



        disableSearchViewInput(binding.searchView1)


        exploreShopAdapter = ExploreShopAdapter(listOf(), this)

        binding.Explorereclyview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = exploreShopAdapter
        }

        categories = mutableListOf()
        categoryAdapter = CategoryAdapter(requireContext(), categories) { category ->
            storeCategoryAndOpenFragment(category)
        }

        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
// Hide or show titles based on product list
        updateTitlesVisibility(menuItems,cartItems,categories)

        fetchCategories()

        fetchShopNameAndUpdateAdapter()

        fetchUserShopName()
        fetchUserShopNames()

        fetchShopName()
        // Rest of your code...
        fetchBuyHistory()
        // Retrieve and display address and locality
        retrieveAddressAndLocality()
        setupRecyclerView()
        setupRecyclerView1()

        binding.dropdown.setOnClickListener {
            showPopupMenu(it)
        }
        binding.tvLocality.setOnClickListener {
            showPopupMenu(it)
        }

        // Assuming discountItems is your list of DiscountItem
        val adapter = HomeDiscountAdapter(requireContext())
        binding.discountrecycler.adapter = adapter
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.discountrecycler.layoutManager = layoutManager


        this@HomeFragment.database = FirebaseDatabase.getInstance()

        return binding.root
    }
    private fun handleNetworkStatus(isConnected: Boolean) {
        if (isConnected) {
            binding.network.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE

        } else {
            binding.network.visibility = View.VISIBLE
            binding.scrollView.visibility = View.GONE
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }
    private fun updateTitlesVisibility(
        menuItems: MutableList<MenuItem>,
        cartItems: MutableList<CartItems>,
        categories: MutableList<Category>
    ) {
        if (menuItems.isEmpty()  &&  cartItems.isEmpty() &&  categories.isEmpty()) {
            binding.textVieww3.visibility = View.INVISIBLE
            binding.textVieww.visibility = View.INVISIBLE
            binding.textVieww1.visibility = View.INVISIBLE
            binding.textVieww2.visibility = View.INVISIBLE
            binding.textVieww4.visibility = View.INVISIBLE
            binding.viewall.visibility = View.INVISIBLE
        } else {
            binding.textVieww3.visibility = View.VISIBLE
            binding.textVieww.visibility = View.VISIBLE
            binding.textVieww1.visibility = View.VISIBLE
            binding.textVieww2.visibility = View.VISIBLE
            binding.textVieww4.visibility = View.VISIBLE
            binding.viewall.visibility = View.VISIBLE
        }
    }


    private fun fetchShopNameAndUpdateAdapter() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("shopname").getValue(String::class.java)
                        shopNames?.let { names ->
                            if (names.isNotBlank()) {
                                val shopNameList = names.split(",") // Split shop names by comma
                                shopNameList.forEach { shopName ->
                                    fetchDiscountItems(shopName.trim())
                                    // Trim whitespace from shop name
                                }
                            } else {
                                // Navigate to LocationNotAvailableActivity
                                startActivity(Intent(context, LocationNotAvailable::class.java))
                                activity?.finish() // Optionally finish current activity
                            }
                        }
                    } else {
                        // Navigate to LocationNotAvailableActivity
                        startActivity(Intent(context, LocationNotAvailable ::class.java))
                        activity?.finish() // Optionally finish current activity
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    Log.e("fetchShopName", "Error fetching shop names", error.toException())
                }
            })
        }
    }

    private fun disableSearchViewInput(searchView: androidx.appcompat.widget.SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.clearFocus()
                findNavController().navigate(R.id.action_homeFragment_to_productSearchFragment)
            }
        }

    }

    private fun fetchDiscountItems(shopName: String) {
        val userId = auth.currentUser?.uid ?: return
        val discountItems = mutableListOf<DiscountItem>()
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"
                    val discountReferences = listOf("discount")
                    discountReferences.forEach { discount ->
                        database.getReference(shopName).child(discount)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapshot in snapshot.children) {
                                            val discountItem = itemSnapshot.getValue(DiscountItem::class.java)
                                            discountItem?.let {
                                                it.path = shopName
                                                val foodNamesList = it.foodNames ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> englishName
                                                }
                                                val combinedName = "$englishName / $languageSpecificName"
                                                it.foodNames = arrayListOf(combinedName)
                                                discountItems.add(it)
                                            }
                                        }
                                        homeDiscountAdapter.updateData(discountItems)
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e("FirebaseError", "Error fetching discount items", error.toException())
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching user language", error.toException())
                }
            })
    }


    private fun fetchCategories() {
        // Check if categories are available in local storage
        val cachedCategories = getCategoriesFromPrefs()
        if (cachedCategories != null && cachedCategories.isNotEmpty()) {
            categories.clear()
            categories.addAll(cachedCategories)
            categoryAdapter.notifyDataSetChanged()
            updateTitlesVisibility(menuItems, cartItems, categories)
            return
        }

        // Fetch from Firebase if local data is not available
        val categoriesRef = database.getReference("Categories")
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    val name = categorySnapshot.key ?: ""
                    val imageUrl = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                    categories.add(Category(name, imageUrl))
                    Log.d("fetchCategories", "Category added: $name with image URL: $imageUrl")
                }
                // Save fetched categories to local storage
                saveCategoriesToPrefs(categories)

                // Notify adapter of data change on the main thread
                requireActivity().runOnUiThread {
                    categoryAdapter.notifyDataSetChanged()
                    updateTitlesVisibility(menuItems, cartItems, categories)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
    private fun saveCategoriesToPrefs(categories: List<Category>) {
        val gson = Gson()
        val json = gson.toJson(categories)
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("categories", json).apply()
    }

    private fun getCategoriesFromPrefs(): List<Category>? {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("categories", null)
        val type = object : TypeToken<List<Category>>() {}.type
        return gson.fromJson(json, type)
    }


    private fun storeCategoryAndOpenFragment(category: Category) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            val userCategoryRef = databaseRef.child("user").child(currentUserId).child("category")
            userCategoryRef.setValue(category.name)
                .addOnSuccessListener {
                    Log.d("HomeFragment", "Category stored successfully: ${category.name}")
                    openFreshFishFragment(category)
                }
                .addOnFailureListener { e ->
                    Log.e("HomeFragment", "Error storing category", e)
                }
        }
    }

    private fun openFreshFishFragment(category: Category) {
        try {
            Log.d("HomeFragment", "Opening FreshFishFragment for category: ${category.name}")
            findNavController().navigate(R.id.action_homeFragment_to_FreshFishFragment)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error opening FreshFishFragment", e)
        }
    }


    private fun fetchBuyHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val userBuyHistoryRef = FirebaseDatabase.getInstance().reference.child("user").child(userId).child("BuyHistory")

        userBuyHistoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = mutableListOf<PreviousItem>()
                for (childSnapshot in snapshot.children) {
                    val foodNamesList = childSnapshot.child("foodNames").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodPricesList = childSnapshot.child("foodPrices").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodImagesList = childSnapshot.child("foodImage").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                    val foodDescriptionList = childSnapshot.child("fooddescription").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})

                    if (foodNamesList != null && foodPricesList != null && foodImagesList != null &&  foodDescriptionList != null) {
                        val foodName = if (foodNamesList.isNotEmpty()) foodNamesList[0] else ""
                        val foodPrice = if (foodPricesList.isNotEmpty()) foodPricesList[0] else ""
                        val foodImage = if (foodImagesList.isNotEmpty()) foodImagesList[0] else ""
                        val foodDescription = if (foodDescriptionList.isNotEmpty())  foodDescriptionList[0] else ""

                        val order = PreviousItem(foodName = foodName, foodPrice = foodPrice, foodImage = foodImage, foodDescription = foodDescription)
                        orders.add(order)
                    }
                }
                adapter1.updateData(orders)
                updateBuyAgainVisibility(orders)

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }
    private fun updateBuyAgainVisibility(orders: List<PreviousItem>) {
        if (orders.isEmpty()) {
            binding.buyy.visibility = View.INVISIBLE

            // Update textVieww constraints to be below category
            val paramsTextVieww = binding.textVieww.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww.topToBottom = R.id.category  // Adjust R.id.category to your actual ID
            binding.textVieww.layoutParams = paramsTextVieww

            // Update textVieww2 constraints to be below textVieww
            val paramsTextVieww2 = binding.viewall.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww2.topToBottom = R.id.category  // Adjust R.id.textVieww to your actual ID
            binding.viewall.layoutParams = paramsTextVieww2

        } else {
            binding.buyy.visibility = View.VISIBLE

            // Restore textVieww constraints to be below buyy
            val paramsTextVieww = binding.textVieww.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww.topToBottom = R.id.buyy  // Adjust R.id.buyy to your actual ID
            binding.textVieww.layoutParams = paramsTextVieww

            // Restore textVieww2 constraints to be below textVieww
            val paramsTextVieww2 = binding.viewall.layoutParams as ConstraintLayout.LayoutParams
            paramsTextVieww2.topToBottom = R.id.buyy  // Adjust R.id.textVieww to your actual ID
            binding.viewall.layoutParams = paramsTextVieww2
        }
    }

    private fun setupRecyclerView() {
        binding.previousrRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter =this@HomeFragment.adapter1
        }
    }
    private fun setupRecyclerView1() {

        adapter = NearItemAdapter(menuItems,cartItems, navController,requireContext())
        binding.Nearitemrecycler.apply {
            layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun fetchShopName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val currentUserId = currentUser.uid
            val locationPath = "Locations/$currentUserId"
            Log.d("FirebaseDebug", "Fetching data from path: $locationPath")
            databaseRef.child(locationPath)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val shopNames = mutableListOf<String>()
                        Log.d("FirebaseDebug", "Data snapshot: ${dataSnapshot.value}")
                        val shopName = dataSnapshot.child("shopname").getValue(String::class.java)
                        Log.d("FirebaseDebug", "Found shop name: $shopName")
                        if (shopName != null) {
                            // Split the shop names by comma and trim any extra spaces
                            val individualShopNames = shopName.split(",").map { it.trim() }
                            shopNames.addAll(individualShopNames)
                        }
                        exploreShopAdapter.setShopList(shopNames)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("FirebaseError", "Error fetching data", databaseError.toException())
                    }
                })
        } else {
            Log.e("FirebaseError", "No authenticated user found.")
        }
    }


    override fun onItemClick(shopName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid

        if (currentUserId != null) {
            val databaseRef =
                FirebaseDatabase.getInstance().getReference("Exploreshop/$currentUserId/ShopName")
            databaseRef.setValue(shopName)
                .addOnSuccessListener {
                    Log.d("FirebaseDebug", "Shop name $shopName stored successfully")
                    findNavController().navigate(R.id.action_homeFragment_to_shoponefragment)
                    // Navigate to next fragment or perform any other action if needed
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseError", "Error storing shop name: $e")
                }
        } else {
            Log.e("FirebaseError", "Current user ID is null")
        }
    }

    override fun onResume() {
        super.onResume()
        fetchOrders()
        fetchShopName()
        loadFromLocalStorage()


    }

    private fun fetchOrders() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.fetchOrders()

        }
    }


    private fun fetchUserShopNames() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("shopname").getValue(String::class.java)
                        if (shopNames.isNullOrEmpty()) {
                            // Shop name is not available, navigate to LocationNotAvailable activity
                            val intent = Intent(requireContext(), LocationNotAvailable::class.java)
                            startActivity(intent)
                            requireActivity().finish() // Optional: Finish current activity
                        } else {
                            val shopNameList = shopNames.split(",") // Split shop names by comma
                            shopNameList.forEach { shopName ->
                                fetchMenuItems(shopName.trim())
                                updateTitlesVisibility(menuItems, cartItems, categories)
                                // Trim whitespace from shop name
                            }
                        }
                    } else {
                        // No snapshot exists, navigate to LocationNotAvailable activity
                        val intent = Intent(requireContext(), LocationNotAvailable::class.java)
                        startActivity(intent)
                        requireActivity().finish() // Optional: Finish current activity
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }
    private fun fetchUserShopName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            databaseReference.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val shopNames = snapshot.child("shopname").getValue(String::class.java)
                        shopNames?.let { names ->
                            val shopNameList = names.split(",") // Split shop names by comma
                            val allMenuItems = mutableListOf<MenuItem>()
                            var processedShops = 0

                            shopNameList.forEach { shopName ->
                                fetchMenuItem(shopName.trim()) { fetchedMenuItems ->
                                    // Add only items from the "menu" path to the overall list
                                    allMenuItems.addAll(fetchedMenuItems)
                                    processedShops++

                                    // Check if all shops have been processed
                                    if (processedShops == shopNameList.size) {
                                        updateTitlesVisibility(allMenuItems, cartItems, categories)
                                        setupRecyclerView2(allMenuItems)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun setupRecyclerView2(menuItems: List<MenuItem>) {
        dealadapter = DealItemAdapter(menuItems, cartItems, requireContext())
        binding.popularRecyclerView1.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dealadapter
        }
    }

    // Fetch menu items for NearItemAdapter
    private fun fetchMenuItems(shopName: String) {
        val userId = auth.currentUser?.uid ?: return
        menuItems.clear()
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    val menuReferences = listOf("menu", "menu1", "menu2")
                    menuReferences.forEach { menu ->
                        database.getReference(shopName).child(menu)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        for (itemSnapshot in snapshot.children) {
                                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                            menuItem?.let {
                                                it.path = shopName
                                                val foodNamesList = it.foodName ?: arrayListOf()
                                                val englishName = foodNamesList.getOrNull(0) ?: ""
                                                val languageSpecificName = when (userLanguage) {
                                                    "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                                    "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                                    "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                                    else -> ""
                                                }

                                                val combinedName = if (languageSpecificName.isNotEmpty() && languageSpecificName != englishName) {
                                                    "$englishName / $languageSpecificName"
                                                } else {
                                                    englishName
                                                }

                                                it.foodName = arrayListOf(combinedName)
                                                menuItems.add(it)
                                            }
                                        }

                                        viewModel.setMenuItem(menuItems)
                                        loadFavoriteItems()

                                        binding.viewall.setOnClickListener {
                                            viewModel.setMenuItems(menuItems)
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    // Handle error
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }


    // Fetch menu items for DealItemAdapter
    private fun fetchMenuItem(shopName: String, onMenuItemsFetched: (List<MenuItem>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val menuItems = mutableListOf<MenuItem>()
        database.getReference("user").child(userId).child("language")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(languageSnapshot: DataSnapshot) {
                    val userLanguage = languageSnapshot.getValue(String::class.java)?.toLowerCase() ?: "english"

                    // Only retrieve the "menu" path for the given shopName
                    val menuReference = database.getReference(shopName).child("menu")

                    menuReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (itemSnapshot in snapshot.children) {
                                    val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                                    menuItem?.let {
                                        it.path = shopName
                                        val foodNamesList = it.foodName ?: arrayListOf()
                                        val englishName = foodNamesList.getOrNull(0) ?: ""
                                        val languageSpecificName = when (userLanguage) {
                                            "tamil" -> foodNamesList.getOrNull(1) ?: ""
                                            "malayalam" -> foodNamesList.getOrNull(2) ?: ""
                                            "telugu" -> foodNamesList.getOrNull(3) ?: ""
                                            else -> englishName // Default to English
                                        }

                                        val combinedName = if (userLanguage == "english") {
                                            englishName
                                        } else {
                                            "$englishName / $languageSpecificName"
                                        }

                                        it.foodName = arrayListOf(combinedName)
                                        menuItems.add(it)
                                    }
                                }
                                onMenuItemsFetched(menuItems)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadFavoriteItems() {
        userId?.let { userId ->
            val databaseRef = FirebaseDatabase.getInstance().getReference("Favourite").child(userId)

            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val favoriteItems = mutableMapOf<String, MenuItem>()
                        snapshot.children.forEach { itemSnapshot ->
                            val favoriteItem = itemSnapshot.getValue(MenuItem::class.java)
                            favoriteItem?.let {
                                it.firebaseKey = itemSnapshot.key // Store the item key
                                it.foodName?.forEach { foodName ->
                                    favoriteItems[foodName] = it
                                }
                            }
                        }
                        updateMenuItemsWithFavorites(favoriteItems)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateMenuItemsWithFavorites(favoriteItems: Map<String, MenuItem>) {
        menuItems.forEach { menuItem ->
            menuItem.foodName?.let { foodNames ->
                foodNames.forEach { foodName ->
                    favoriteItems[foodName]?.let { favoriteItem ->
                        menuItem.favorite = favoriteItem.favorite
                        menuItem.firebaseKey = favoriteItem.firebaseKey // Store the key in the menu item
                    }
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
    private fun observeViewModel() {
        viewModel.address.observe(viewLifecycleOwner) { address ->
            binding.tvAddress.text = address
        }

        viewModel.locality.observe(viewLifecycleOwner) { locality ->
            binding.tvLocality.text = locality
        }
        viewModel.latitude.observe(viewLifecycleOwner) { latitude ->
            binding.tvLatitude.text = latitude.toString()
        }
        viewModel.longitude.observe(viewLifecycleOwner) { longitude ->
            binding.tvLogitude.text = longitude.toString()
        }


    }


    private fun retrieveAddressAndLocality() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val userLocationRef = databaseReference.child(uid)
            userLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val address = snapshot.child("address").getValue(String::class.java)
                        val locality = snapshot.child("locality").getValue(String::class.java)
                        val latitude = snapshot.child("latitude").getValue(Double::class.java)
                        val longitude = snapshot.child("longitude").getValue(Double::class.java)

                        if (isAdded) {
                            saveToLocalStorage(address, locality, latitude?.toString(), longitude?.toString())
                            updateUI(address, locality, latitude, longitude)
                        } else {
                            // Save data to local storage but defer UI update
                            saveToLocalStorage(address, locality, latitude?.toString(), longitude?.toString())
                        }
                    } else {
                        // Handle case where data doesn't exist
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    private fun updateUI(address: String?, locality: String?, latitude: Double?, longitude: Double?) {
        binding.tvAddress.text = address
        binding.tvLocality.text = locality
        binding.tvLatitude.text = latitude?.toString() ?: "N/A"
        binding.tvLogitude.text = longitude?.toString() ?: "N/A"
    }

    private fun saveToLocalStorage(address: String?, locality: String?, latitude: String?, longitude: String?) {
        if (!isAdded) {
            // Fragment is not attached to an activity, abort the operation
            Log.e("AccountFragment", "Fragment is not attached to an activity. Cannot save to local storage.")
            return
        }

        val sharedPreferences = requireActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("address", address)
        editor.putString("locality", locality)
        editor.putString("latitude", latitude)
        editor.putString("longitude", longitude)
        editor.apply()
    }
    private fun loadFromLocalStorage() {
        val sharedPreferences = requireActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE)
        val address = sharedPreferences.getString("address", "N/A")
        val locality = sharedPreferences.getString("locality", "N/A")
        val latitude = sharedPreferences.getString("latitude", "N/A")
        val longitude = sharedPreferences.getString("longitude", "N/A")

        binding.tvAddress.text = address
        binding.tvLocality.text = locality
        binding.tvLatitude.text = latitude
        binding.tvLogitude.text = longitude
    }
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.address, popupMenu.menu)

        // Set text color for the PopupMenu items
        val menu = popupMenu.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val spannableString = SpannableString(menuItem.title.toString())
            spannableString.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.navy)),
                0,
                spannableString.length,
                0
            )
            menuItem.title = spannableString
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.use_current_location -> {
                    // Handle "Use Current Location" click
                    val intent = Intent(requireContext(), LocationActivity::class.java)
                    startActivity(intent)
                    // Hide the dropdown icon after picking an address
                    true
                }
                // Add more saved address clicks as needed
                else -> false
            }
        }

        popupMenu.setOnDismissListener {
            // Dismiss the PopupMenu when it's dismissed
            popupMenu.dismiss()
        }

        popupMenu.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupImageSlider()
        loadFromLocalStorage()

        binding.viewall.setOnClickListener {
            // Calculate the offset of textVieww1 relative to the top of scrollView
            val offset = binding.Nearitemrecycler.bottom
            // Scroll scrollView to the position of textVieww1
            binding.scrollView.smoothScrollTo(0, offset)
        }
    }

    private fun setupImageSlider() {
        val imageList = ArrayList<SlideModel>()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("Banners")

        // List all images under "Banners" path
        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                listResult.items.forEach { item ->
                    // Get download URL for each image
                    item.downloadUrl.addOnSuccessListener { uri ->
                        // Add each image to the imageList
                        imageList.add(SlideModel(uri.toString(), scaleType = ScaleTypes.FIT))
                        // Set imageList to the image slider
                        binding.imageSlider.setImageList(imageList)
                    }.addOnFailureListener { exception ->
                        // Handle any errors during getting download URL

                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors during listing images

            }

        val imageSlide = binding.imageSlider
        imageSlide.setItemClickListener(object : ItemClickListener {
            override fun doubleClick(position: Int) {
                // Double click listener implementation
            }

            override fun onItemSelected(position: Int) {
                // Check if the selected image is "ban"
            }
        })
    }
}