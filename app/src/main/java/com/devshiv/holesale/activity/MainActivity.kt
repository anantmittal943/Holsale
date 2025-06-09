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
//                checkLocationEnabled()
                /*fetchCurrentAddress { address ->
                    addAddressEdTxt.setText(address)
                    Log.d(TAG, "address: $address")
                }*/
//                dialog.show()
                val intent = Intent(this, AddListActivity::class.java)
//                activityResultLauncher.launch(intent)
                startActivityForResult(intent, ADD_LIST_REQUEST_CODE)
//                onPause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        skeleton.showSkeleton()
        getData()
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
                fetchCurrentAddress { address ->
//                    addAddressEdTxt.setText(address)
                    Log.d(TAG, "address: $address")
                }
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

    private fun uploadImg(fileUri: Uri, phoneNo: String, address: String) {
        binding.noDataImg.visibility = View.GONE
        binding.startAddingTxt.visibility = View.GONE
        binding.listRV.visibility = carbon.view.View.VISIBLE
        skeleton.showSkeleton()
        val uid = firebaseAuth.currentUser?.uid ?: return
        val fileRef = storage.reference.child("users/$uid/${System.currentTimeMillis()}.jpg")
        if (fileUri.toString() != oldImageUrl) {
            fileRef.putFile(fileUri)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d(TAG, "Image uploaded: $downloadUri")
                        if (isEditing) {
                            updateData(downloadUri, phoneNo, address, docId)
                            isEditing = !isEditing
                        } else {
                            addData(downloadUri, phoneNo, address)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Image upload failed", it)
                    binding.noDataImg.visibility = View.VISIBLE
                    binding.startAddingTxt.visibility = View.VISIBLE
                    binding.listRV.visibility = carbon.view.View.GONE
                }
        } else {
            updateData(oldImageUrl.toUri(), phoneNo, address, docId)
            isEditing = false
        }
    }

    private fun addData(imageUrl: Uri, phoneNo: String, address: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        val listModel = ListModel(
            imageUrl = imageUrl.toString(),
            phoneNumber = phoneNo,
            address = address,
            createdAt = ListModel.getCurrentTime(),
            updatedAt = ListModel.getCurrentTime(),
        )

        firestore.collection("list")
            .document(uid)
            .collection("items")
            .add(listModel)
            .addOnSuccessListener {
                Toast.makeText(this, "Added successfully", Toast.LENGTH_SHORT).show()
                binding.noDataImg.visibility = View.GONE
                binding.startAddingTxt.visibility = View.GONE
                binding.listRV.visibility = View.VISIBLE
                Log.d(TAG, "Data saved")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to save data", it)
                binding.noDataImg.visibility = View.VISIBLE
                binding.startAddingTxt.visibility = View.VISIBLE
                binding.listRV.visibility = carbon.view.View.GONE
            }
        getData()
    }

    private fun deleteData(item: ListModel) {
        skeleton.showSkeleton()
        val uid = firebaseAuth.currentUser?.uid ?: return
        val docId = item.id ?: return

        firestore.collection("list")
            .document(uid)
            .collection("items")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
                val storageRef = storage.getReferenceFromUrl(item.imageUrl!!)
                storageRef.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Image deleted from storage")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete image from storage", e)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        getData()
    }

    private fun updateData(newImageUrl: Uri, newPhone: String, newAddress: String, docId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        val updatedModel = mapOf(
            "imageUrl" to newImageUrl.toString(),
            "phoneNumber" to newPhone,
            "address" to newAddress,
            "updatedAt" to ListModel.getCurrentTime()
        )

        firestore.collection("list")
            .document(uid)
            .collection("items")
            .document(docId)
            .update(updatedModel)
            .addOnSuccessListener {
                Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
                if (newImageUrl.toString() != oldImageUrl) {
                    val storageRef = storage.getReferenceFromUrl(oldImageUrl)
                    storageRef.delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "Image deleted from storage")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to delete image from storage", e)
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
        getData()
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

    private fun fetchCurrentAddress(onAddressFetched: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingAddressCallback = onAddressFetched
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ADDRESS_LOCATION_PERMISSION_REQUEST_CODE
            )
            Log.e(TAG, "fetchCurrentAddress: permission not granted or denied")
            return
        } else {
            val fusedClientLocation = LocationServices.getFusedLocationProviderClient(this)
            fusedClientLocation.getLastLocation()
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0].getAddressLine(0)
                            onAddressFetched(address)
                        } else {
                            Toast.makeText(this, "Address not found!!", Toast.LENGTH_SHORT).show()
                            onAddressFetched("")
                            addAddressEdTxt.hint = "Add address manually"
                        }
                    } else {
//                        checkLocationEnabled()
//                        Toast.makeText(this, "Location is null!!", Toast.LENGTH_SHORT).show()
                        onAddressFetched("")
                        addAddressEdTxt.hint = "Add address manually"
                    }
                }
                .addOnFailureListener {
//                    checkLocationEnabled()
                    Log.e("Location", "Failed to get location", it)
                    Toast.makeText(this, "Failed to get Location", Toast.LENGTH_SHORT).show()
                    onAddressFetched("")
                    addAddressEdTxt.hint = "Add address manually"
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == ADDRESS_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingAddressCallback?.let {
                    fetchCurrentAddress(it)
                    pendingAddressCallback = null
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                addAddressEdTxt.hint = "Add address manually"
                pendingAddressCallback?.invoke("")
                pendingAddressCallback = null
            }
        }
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
            fetchCurrentAddress { address ->
//                addAddressEdTxt.setText(address)
                Log.d(TAG, "address: $address")
            }
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
        private const val ADDRESS_LOCATION_PERMISSION_REQUEST_CODE = 1010
        private const val REQUEST_LOCATION_CODE = 101
        private const val ADD_LIST_REQUEST_CODE = 9981
    }

}
