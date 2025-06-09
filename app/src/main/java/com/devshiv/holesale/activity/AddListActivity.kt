package com.devshiv.holesale.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.devshiv.holesale.R
import com.devshiv.holesale.TokenProvider
import com.devshiv.holesale.Utils
import com.devshiv.holesale.adapter.MainAdapter
import com.devshiv.holesale.bottom_sheet.AddressBottomSheetFragment
import com.devshiv.holesale.databinding.ActivityAddListBinding
import com.devshiv.holesale.model.ListModel
import com.devshiv.holesale.model.UserModel
import com.faltenreich.skeletonlayout.Skeleton
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.Locale
import kotlin.math.log

class AddListActivity : AppCompatActivity() {

    private val binding: ActivityAddListBinding by lazy {
        ActivityAddListBinding.inflate(layoutInflater)
    }
    private val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage("gs://holsale-61ea5.firebasestorage.app")
    private var uploadImageUrl: Uri? = null
    private var pendingAddressCallback: ((String) -> Unit)? = null
    private var isEditing = false
    private var docId: String = ""
    private var oldImageUrl: String = ""
    private val TAG = "xyz"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        enableEdgeToEdge()

        setContentView(binding.root)

        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/

        Utils.changeSystemBarsColor(
            window, binding.root,
            ContextCompat.getColor(this, R.color.colorPrimaryVariant),
            ContextCompat.getColor(this, R.color.colorPrimaryVariant)
        )

        Places.setPlacesAppCheckTokenProvider(TokenProvider())

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        val selectedItem = intent.getSerializableExtra("selectedItem") as? ListModel

        binding.uploadBtn.setOnClickListener {
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

        binding.positiveBtn.setOnClickListener {
            val phoneNo = binding.addPhoneNoEdTxt.text.toString()
            val address = binding.addAddressEdTxt.text.toString()
            if (uploadImageUrl != null && phoneNo.isNotEmpty() && address.isNotEmpty()) {
                Log.d(TAG, "onCreate: $uploadImageUrl")
                uploadImg(uploadImageUrl!!, phoneNo, address)
                onBackPressed()
//                dialog.dismiss()
            } else {
                Log.e(TAG, "Either image not uploaded or phone is empty or address is empty")
                Toast.makeText(this, "Make sure to fill all details!!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.addAddressEdTxt.setOnClickListener {
            /*AddressBottomSheetFragment().show(
                supportFragmentManager,
                "AddressBottomSheet"
            )*/
            binding.addPhoneNoEdTxt.clearFocus()
            val fields = listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this@AddListActivity)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        if (selectedItem != null) {
            binding.topTxt.text = "Review Order"
            binding.editBtn.visibility = View.VISIBLE
            binding.editBtn.setOnClickListener {
                isEditing = true
                binding.topTxt.text = "Edit your Order"
                binding.suggestionTxt.text = getString(R.string.suggestionTxt_edit)
                binding.editBtn.visibility = View.GONE
                binding.deleteBtn.visibility = View.GONE
                binding.imagePreview.alpha = 0.3f
                binding.uploadSuggestionTxt.text = "Tap to change photo of your order list"
                binding.uploadSuggestionTxt.visibility = View.VISIBLE
                binding.uploadBtn.text = "Change Photo"
                binding.uploadBtn.visibility = View.VISIBLE
                binding.addPhoneNoTxt.text = "Change Phone Number"
                binding.addPhoneNoEdTxt.isEnabled = true
                binding.addPhoneNoEdTxt.setText(selectedItem.phoneNumber)
                binding.addAddressTxt.text = "Change Address"
                binding.addAddressEdTxt.isEnabled = true
                binding.addAddressEdTxt.text = selectedItem.address
                binding.positiveBtn.text = "Update"
                binding.positiveBtn.visibility = View.VISIBLE
                uploadImageUrl = selectedItem.imageUrl?.toUri()
                oldImageUrl = selectedItem.imageUrl!!
                docId = selectedItem.id!!
                binding.createdAtTxt.visibility = View.GONE
                binding.updatedAtTxt.visibility = View.GONE
                binding.fl1.visibility = View.GONE
                binding.fl2.visibility = View.GONE
                binding.createdDateTxt.visibility = View.GONE
                binding.createdTimeTxt.visibility = View.GONE
                binding.updatedDateTxt.visibility = View.GONE
                binding.updatedTimeTxt.visibility = View.GONE
            }
            binding.deleteBtn.visibility = View.VISIBLE
            binding.deleteBtn.setOnClickListener {
                deleteData(selectedItem)
//                onBackPressed()
            }
            binding.suggestionTxt.text = getString(R.string.suggestionTxt_preview)
            binding.uploadSuggestionTxt.visibility = View.GONE
            binding.uploadBtn.visibility = View.GONE
            binding.imagePreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(selectedItem.imageUrl)
                .placeholder(R.drawable.ic_image_not_found)
                .into(binding.imagePreview)
            binding.addPhoneNoTxt.text = "Phone Number"
            binding.addPhoneNoEdTxt.isEnabled = false
            binding.addPhoneNoEdTxt.setText(selectedItem.phoneNumber)
            binding.addAddressTxt.text = "Address"
            binding.addAddressEdTxt.isEnabled = false
            binding.addAddressEdTxt.text = selectedItem.address
            binding.positiveBtn.visibility = View.GONE
            binding.createdAtTxt.visibility = View.VISIBLE
            binding.updatedAtTxt.visibility = View.VISIBLE
            binding.fl1.visibility = View.VISIBLE
            binding.fl2.visibility = View.VISIBLE
            binding.createdDateTxt.text = ListModel.formatDate(selectedItem.createdAt)
            binding.createdDateTxt.visibility = View.VISIBLE
            binding.createdTimeTxt.text = ListModel.formatTime(selectedItem.createdAt)
            binding.createdTimeTxt.visibility = View.VISIBLE
            binding.updatedDateTxt.text = ListModel.formatDate(selectedItem.updatedAt)
            binding.updatedDateTxt.visibility = View.VISIBLE
            binding.updatedTimeTxt.text = ListModel.formatTime(selectedItem.updatedAt)
            binding.updatedTimeTxt.visibility = View.VISIBLE
        } else {
            binding.topTxt.text = "Order from Photo"
            binding.editBtn.visibility = View.GONE
            binding.deleteBtn.visibility = View.GONE
            binding.suggestionTxt.text = getString(R.string.suggestionTxt_add)
            binding.uploadSuggestionTxt.visibility = View.VISIBLE
            binding.uploadBtn.visibility = View.VISIBLE
            binding.imagePreview.visibility = View.GONE
            binding.addPhoneNoTxt.text = "Add Phone Number"
            binding.addPhoneNoEdTxt.isEnabled = true
            binding.addPhoneNoEdTxt.setText("")
//            binding.addPhoneNoEdTxt.clearFocus()
            binding.addAddressTxt.text = "Address"
            binding.addAddressEdTxt.isEnabled = true
            binding.addAddressEdTxt.text = "Search for address"
            binding.positiveBtn.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == IMAGE_PICKER_REQUEST_CODE) {
            uploadImageUrl = data?.data ?: return
            Log.d(TAG, "onActivityResult: $uploadImageUrl")
            binding.uploadSuggestionTxt.visibility = View.GONE
            binding.uploadBtn.visibility = View.GONE
            binding.imagePreview.visibility = View.VISIBLE
            binding.imagePreview.alpha = 1f
            Glide.with(this)
                .load(uploadImageUrl)
                .placeholder(R.drawable.ic_image_not_found)
                .into(binding.imagePreview)
        }

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(it)
                        binding.addAddressEdTxt.setText(place.address)
                    }
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Toast.makeText(this, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
                }

                Activity.RESULT_CANCELED -> {
                    // No action
                }
            }
        }
    }

    private fun uploadImg(fileUri: Uri, phoneNo: String, address: String) {
//        binding.noDataImg.visibility = View.GONE
//        binding.startAddingTxt.visibility = View.GONE
//        binding.listRV.visibility = carbon.view.View.VISIBLE
//        skeleton.showSkeleton()
        val resultIntent = Intent()
        resultIntent.putExtra("action", "show_loading")
        setResult(Activity.RESULT_OK, resultIntent)
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
                            Log.d(TAG, "uploadImg add func called\n$uid")
                            addData(downloadUri, phoneNo, address)
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Image upload failed", it)
//                    binding.noDataImg.visibility = View.VISIBLE
//                    binding.startAddingTxt.visibility = View.VISIBLE
//                    binding.listRV.visibility = carbon.view.View.GONE
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
//                setResult(RESULT_OK)
                val resultIntent = Intent()
                resultIntent.putExtra("action", "refresh")
                setResult(Activity.RESULT_OK, resultIntent)
//                finish()
//                binding.noDataImg.visibility = View.GONE
//                binding.startAddingTxt.visibility = View.GONE
//                binding.listRV.visibility = View.VISIBLE
                Log.d(TAG, "Data saved")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to save data", it)
//                binding.noDataImg.visibility = View.VISIBLE
//                binding.startAddingTxt.visibility = View.VISIBLE
//                binding.listRV.visibility = carbon.view.View.GONE
            }
//        getData()
//        onBackPressed()
    }

    private fun deleteData(item: ListModel) {
//        skeleton.showSkeleton()
        val uid = firebaseAuth.currentUser?.uid ?: return
        val docId = item.id ?: return

        firestore.collection("list")
            .document(uid)
            .collection("items")
            .document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show()
//                setResult(RESULT_OK)
                val resultIntent = Intent()
                resultIntent.putExtra("action", "refresh")
                setResult(Activity.RESULT_OK, resultIntent)
//                finish()
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
//        getData()
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
//                setResult(RESULT_OK)
                val resultIntent = Intent()
                resultIntent.putExtra("action", "refresh")
                setResult(Activity.RESULT_OK, resultIntent)
//                finish()
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
//        getData()
    }

    companion object {
        private const val IMAGE_PICKER_REQUEST_CODE = 1001
        private const val AUTOCOMPLETE_REQUEST_CODE = 9981
    }
}