package com.devshiv.holesale.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.devshiv.holesale.databinding.ActivityMainBinding
import java.util.Locale
import com.devshiv.holesale.model.ListModel
import com.devshiv.holesale.adapter.MainAdapter
import com.devshiv.holesale.R
import com.devshiv.holesale.Utils
import com.devshiv.holesale.model.UserModel
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient

class MainActivity : ComponentActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage("gs://holsale-61ea5.firebasestorage.app")
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var skeleton: Skeleton
    private lateinit var userModel: UserModel
    private var listModel: MutableList<ListModel> = mutableListOf()
    private lateinit var mainAdapter: MainAdapter
    private var uploadImageUrl: Uri? = null
    private var pendingAddressCallback: ((String) -> Unit)? = null
    private lateinit var uploadBtn: ImageView
    private lateinit var addAddressEdTxt: EditText
    private var isEditing = false
    private var docId: String = ""
    private var oldImageUrl: String = ""
    private val TAG = "xyz"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        Utils.changeSystemBarsColor(
            window, binding.root,
            ContextCompat.getColor(this, R.color.colorPrimaryVariant),
            ContextCompat.getColor(this, R.color.colorPrimaryVariant)
        )

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {

            binding.nameTxt.text = "Hi, ${currentUser.displayName}"

            binding.searchBtn.setOnClickListener {
                binding.searchBox.visibility = View.VISIBLE
                binding.searchBtn.visibility = View.GONE
                binding.closeBtn.visibility = View.VISIBLE
                binding.searchBox.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchBox, InputMethodManager.SHOW_IMPLICIT)
            }
            binding.closeBtn.setOnClickListener {
                binding.searchBox.setText("")
                binding.searchBox.visibility = View.GONE
                binding.closeBtn.visibility = View.GONE
                binding.searchBtn.visibility = View.VISIBLE
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
                getData()
            }
            binding.searchBox.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
                    true
                } else {
                    false
                }
            }
            binding.searchBox.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterList(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            mainAdapter = MainAdapter(listModel, this) { selectedItem ->
                val intent = Intent(this, AddListActivity::class.java)
                intent.putExtra("selectedItem", selectedItem)
                startActivity(intent)
            }

            val layoutManager = LinearLayoutManager(this)
            binding.listRV.layoutManager = layoutManager
            binding.listRV.adapter = mainAdapter
            skeleton = binding.listRV.applySkeleton(R.layout.main_item_layout)
            skeleton.maskColor = ContextCompat.getColor(this, carbon.R.color.carbon_blueGrey_200)
            skeleton.shimmerColor = ContextCompat.getColor(this, R.color.colorSecondary)
            skeleton.showShimmer = true

            /*activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
//                    getData()
                    val action = result.data?.getStringExtra("action")
                    when (action) {
                        "refresh" -> getData()
                        "show_loading" -> skeleton.showSkeleton()
                    }
                }
            }*/

            binding.addListBtn.setOnClickListener {
                checkLocationEnabled()
                /*fetchCurrentAddress { address ->
                    addAddressEdTxt.setText(address)
                    Log.d(TAG, "address: $address")
                }*/
//                dialog.show()
//                val intent = Intent(this, AddListActivity::class.java)
//                activityResultLauncher.launch(intent)
//                startActivityForResult(intent, ADD_LIST_REQUEST_CODE)
//                onPause()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
        skeleton.showSkeleton()
        getData()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
//        skeleton.showSkeleton()
//        getData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICKER_REQUEST_CODE) {
            uploadImageUrl = data?.data ?: return
            Log.d(TAG, "onActivityResult: $uploadImageUrl")
            Glide.with(this)
                .load(uploadImageUrl)
                .into(uploadBtn)
        }
        if (requestCode == REQUEST_LOCATION_CODE) {
            if (resultCode == RESULT_OK) {
                val intent = Intent(this, AddListActivity::class.java)
//                activityResultLauncher.launch(intent)
                startActivityForResult(intent, ADD_LIST_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Please turn on location services", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == ADD_LIST_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val action = data?.getStringExtra("action")
                when (action) {
                    "refresh" -> getData()
                    "show_loading" -> skeleton.showSkeleton()
                }
            } else {
                getData()
            }
        }
    }

    private fun getData() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        firestore.collection("list")
            .document(uid)
            .collection("items")
            .get()
            .addOnSuccessListener { result ->
                listModel.clear()
                for (document in result) {
                    val model = document.toObject(ListModel::class.java).copy(id = document.id)
                    Log.d(TAG, "getData: ${document.id}\n${document.data}")
                    listModel.add(model)
                }
                if (listModel.isNotEmpty()) {
                    skeleton.showOriginal()
                    binding.noDataImg.visibility = View.GONE
                    binding.startAddingTxt.visibility = View.GONE
                    binding.listRV.visibility = carbon.view.View.VISIBLE
                    mainAdapter.notifyDataSetChanged()
                } else {
                    skeleton.showOriginal()
                    binding.noDataImg.visibility = View.VISIBLE
                    binding.startAddingTxt.visibility = View.VISIBLE
                    binding.listRV.visibility = carbon.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                skeleton.showOriginal()
                binding.noDataImg.visibility = View.VISIBLE
                binding.startAddingTxt.visibility = View.VISIBLE
                binding.listRV.visibility = carbon.view.View.GONE
                Log.e(TAG, "getData: exception", e)
            }
    }

    private fun filterList(query: String) {
        val filteredList = listModel.filter {
            it.phoneNumber?.contains(query, ignoreCase = true) == true
        }
        mainAdapter.updateList(filteredList)
    }

    override fun onBackPressed() {
        if (binding.searchBox.isVisible) {
            binding.searchBox.setText("")
            binding.searchBox.visibility = View.GONE
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchBox.windowToken, 0)
            getData()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkLocationEnabled() {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // shows dialog even if previously cancelled

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            val intent = Intent(this, AddListActivity::class.java)
//                activityResultLauncher.launch(intent)
            startActivityForResult(intent, ADD_LIST_REQUEST_CODE)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, REQUEST_LOCATION_CODE)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e("LocationCheck", "Error showing dialog: ${sendEx.message}")
                }
            } else {
                Toast.makeText(this, "Location is required but not available", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 1001
        private const val REQUEST_LOCATION_CODE = 101
        private const val ADD_LIST_REQUEST_CODE = 9981
    }

}
