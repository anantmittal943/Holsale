package com.devshiv.holesale.activity

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.activity.enableEdgeToEdge
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
import com.devshiv.holesale.model.UserModel
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.applySkeleton

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage("gs://holsale-61ea5.firebasestorage.app")
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
    private var showSkeleton: Boolean = true
    private val TAG = "xyz"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryVariant)

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            userModel = UserModel(
                currentUser.uid,
                currentUser.displayName,
                currentUser.email,
                currentUser.phoneNumber,
                currentUser.photoUrl,
                currentUser.isEmailVerified
            )
            val docRef = firestore.collection("users").document(currentUser.uid)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        docRef.set(userModel)
                            .addOnSuccessListener { Log.d(TAG, "User saved") }
                            .addOnFailureListener { Log.e(TAG, "Error saving user", it) }
                    } else {
                        Log.d(TAG, "User already exists, not saving again.")
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error checking user", it)
                }

            binding.nameTxt.text = "Hi, ${currentUser.displayName}"

            binding.searchBtn.setOnClickListener {
                binding.searchBox.visibility = View.VISIBLE
                binding.searchBox.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.searchBox, InputMethodManager.SHOW_IMPLICIT)
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
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterList(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })


            val builder = AlertDialog.Builder(this)
            val customLayout: View = layoutInflater.inflate(R.layout.list_item_layout, null)
            builder.setView(customLayout)
            val dialog = builder.create()
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(R.drawable.custom_alert_dialog)
            val editBtn: ImageView = customLayout.findViewById(R.id.editBtn)
            val deleteBtn: ImageView = customLayout.findViewById(R.id.deleteBtn)
            val uploadImgTxt: TextView = customLayout.findViewById(R.id.uploadImageTxt)
            val addPhoneNoTxt: TextView = customLayout.findViewById(R.id.addPhoneNoTxt)
            val addAddressTxt: TextView = customLayout.findViewById(R.id.addAddressTxt)
            val addPhoneNoEdTxt: EditText = customLayout.findViewById(R.id.addPhoneNoEdTxt)
            addAddressEdTxt = customLayout.findViewById(R.id.addAddressEdTxt)
            uploadBtn = customLayout.findViewById(R.id.uploadBtn)
            val positiveBtn: Button = customLayout.findViewById(R.id.positiveBtn)
            val negativeBtn: Button = customLayout.findViewById(R.id.negativeBtn)
            val createdAtTxt: TextView = customLayout.findViewById(R.id.createdAtTxt)
            val updatedAtTxt: TextView = customLayout.findViewById(R.id.updatedAtTxt)
            val createdDateTxt: TextView = customLayout.findViewById(R.id.createdDateTxt)
            val createdTimeTxt: TextView = customLayout.findViewById(R.id.createdTimeTxt)
            val updatedDateTxt: TextView = customLayout.findViewById(R.id.updatedDateTxt)
            val updatedTimeTxt: TextView = customLayout.findViewById(R.id.updatedTimeTxt)
            uploadBtn.setOnClickListener {
                ImagePicker.with(this)
                    .galleryMimeTypes(
                        mimeTypes = arrayOf(
                            "image/png",
                            "image/jpg",
                            "image/jpeg"
                        )
                    )
                    .crop()
                    .compress(1024)
                    .maxResultSize(1080, 1080)
                    .start(IMAGE_PICKER_REQUEST_CODE)
            }
            positiveBtn.setOnClickListener {
                val phoneNo = addPhoneNoEdTxt.text.toString()
                val address = addAddressEdTxt.text.toString()
                if (uploadImageUrl != null && phoneNo.isNotEmpty() && address.isNotEmpty()) {
                    Log.d(TAG, "onCreate: $uploadImageUrl")
                    uploadImg(uploadImageUrl!!, phoneNo, address)
                    dialog.dismiss()
                } else {
                    Log.e(TAG, "Either image not uploaded or phone is empty or address is empty")
                    Toast.makeText(this, "Make sure to fill all details!!", Toast.LENGTH_SHORT).show()
                }
            }
            negativeBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                uploadImgTxt.setText("Upload Image")
                addPhoneNoTxt.setText("Add Phone Number")
                addAddressTxt.setText("Add Address")
                editBtn.visibility = View.GONE
                deleteBtn.visibility = View.GONE
                addPhoneNoEdTxt.setText("")
                addPhoneNoEdTxt.clearFocus()
                addPhoneNoEdTxt.isEnabled = true
                addAddressEdTxt.setText("")
                addAddressEdTxt.clearFocus()
                addAddressEdTxt.isEnabled = true
                uploadBtn.setImageResource(R.drawable.upload)
                uploadBtn.isClickable = true
                positiveBtn.setText("Add")
                positiveBtn.visibility = View.VISIBLE
                negativeBtn.setText("Cancel")
//                binding.searchBox.setText("")
//                binding.searchBox.visibility = View.GONE
                createdAtTxt.visibility = View.GONE
                createdDateTxt.visibility = View.GONE
                createdTimeTxt.visibility = View.GONE
                updatedAtTxt.visibility = View.GONE
                updatedDateTxt.visibility = View.GONE
                updatedTimeTxt.visibility = View.GONE
//                getData()
            }
            binding.addListBtn.setOnClickListener {
                fetchCurrentAddress { address ->
                    addAddressEdTxt.setText(address)
                    Log.d(TAG, "address: $address")
                }
                dialog.show()
            }

            mainAdapter = MainAdapter(listModel, this) { selectedItem ->
                createdAtTxt.visibility = View.VISIBLE
                createdDateTxt.visibility = View.VISIBLE
                createdTimeTxt.visibility = View.VISIBLE
                updatedAtTxt.visibility = View.VISIBLE
                updatedDateTxt.visibility = View.VISIBLE
                updatedTimeTxt.visibility = View.VISIBLE
                createdDateTxt.text = ListModel.formatDate(selectedItem.createdAt)
                createdTimeTxt.text = ListModel.formatTime(selectedItem.createdAt)
                updatedDateTxt.text = ListModel.formatDate(selectedItem.updatedAt)
                updatedTimeTxt.text = ListModel.formatTime(selectedItem.updatedAt)
                uploadImgTxt.setText("Image Uploaded")
                addPhoneNoTxt.setText("Phone Number")
                addAddressTxt.setText("Address")
                editBtn.visibility = View.VISIBLE
                deleteBtn.visibility = View.VISIBLE
                positiveBtn.visibility = View.GONE
                negativeBtn.setText("Close")
                uploadBtn.isClickable = false
                addPhoneNoEdTxt.isEnabled = false
                addAddressEdTxt.isEnabled = false
                Glide.with(this)
                    .load(selectedItem.imageUrl)
                    .placeholder(R.drawable.ic_image_not_found)
                    .into(uploadBtn)
                addPhoneNoEdTxt.setText(selectedItem.phoneNumber)
                addAddressEdTxt.setText(selectedItem.address)
                dialog.show()
                editBtn.setOnClickListener {
                    isEditing = true
                    uploadImgTxt.setText("Change Image")
                    addPhoneNoTxt.setText("Change Phone Number")
                    addAddressTxt.setText("Change Address")
                    uploadBtn.isClickable = true
//                    uploadBtn.setImageResource(R.drawable.upload)
                    addPhoneNoEdTxt.isEnabled = true
                    addAddressEdTxt.isEnabled = true
                    positiveBtn.setText("Update")
                    positiveBtn.visibility = View.VISIBLE
                    negativeBtn.setText("Cancel")
                    docId = selectedItem.id!!
                    uploadImageUrl = selectedItem.imageUrl?.toUri()
                    oldImageUrl = selectedItem.imageUrl!!
                }
                deleteBtn.setOnClickListener {
                    deleteData(selectedItem)
                    dialog.dismiss()
                }
            }
            val layoutManager = LinearLayoutManager(this)
            binding.listRV.layoutManager = layoutManager
            binding.listRV.adapter = mainAdapter
            skeleton = binding.listRV.applySkeleton(R.layout.main_item_layout)
            skeleton.maskColor = ContextCompat.getColor(this, carbon.R.color.carbon_blueGrey_200)
            skeleton.shimmerColor = ContextCompat.getColor(this, R.color.colorSecondary)
            skeleton.showShimmer = true
        }
    }

    override fun onStart() {
        super.onStart()
        skeleton.showSkeleton()
    }

    override fun onResume() {
        super.onResume()
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
    }

    private fun uploadImg(fileUri: Uri, phoneNo: String, address: String) {
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
                Log.d(TAG, "Data saved")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to save data", it)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pendingAddressCallback = onAddressFetched
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            Log.e(TAG, "fetchCurrentAddress: permission not granted or denied")
            return
        } else {
            val fusedClientLocation = LocationServices.getFusedLocationProviderClient(this)
            fusedClientLocation.getLastLocation()
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0].getAddressLine(0)
                            onAddressFetched(address)
                        } else {
                            Toast.makeText(this, "Address not found!!", Toast.LENGTH_SHORT).show()
                            onAddressFetched("")
                            addAddressEdTxt.hint = "Add address manually"
                        }
                    } else {
                        Toast.makeText(this, "Location is null!!", Toast.LENGTH_SHORT).show()
                        onAddressFetched("")
                        addAddressEdTxt.hint = "Add address manually"
                    }
                }
                .addOnFailureListener {
                    Log.e("Location", "Failed to get location", it)
                    Toast.makeText(this, "Failed to get Location", Toast.LENGTH_SHORT).show()
                    onAddressFetched("")
                    addAddressEdTxt.hint = "Add address manually"
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
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


    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1010
    }

}
