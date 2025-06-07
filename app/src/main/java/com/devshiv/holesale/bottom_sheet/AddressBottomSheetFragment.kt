package com.devshiv.holesale.bottom_sheet

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devshiv.holesale.BuildConfig
import com.devshiv.holesale.Utils.Companion.TAG
import com.devshiv.holesale.databinding.FragmentAddressBottomSheetBinding
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddressBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAddressBottomSheetBinding
    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddressBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val apiKey = BuildConfig.MAPS_API_KEY
        Places.initializeWithNewPlacesApiEnabled(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())
        binding.addressEdTxt.setOnClickListener {
            launchAutoComplete()
        }
    }

    private fun launchAutoComplete() {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext())
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    Log.d(TAG, "onActivityResult: ${resultCode} auth hogya chl gya")
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    binding.addressEdTxt.setText(place.address)
                }

                AutocompleteActivity.RESULT_ERROR -> {
                    val status = Autocomplete.getStatusFromIntent(data!!)
                    Log.e(TAG, "onActivityResult: auto complete result $status\n${status.statusMessage}")
                    Toast.makeText(requireContext(), status.statusMessage, Toast.LENGTH_SHORT).show()
                }

                AppCompatActivity.RESULT_CANCELED -> {
                    // user canceled
                    Log.e(TAG, "onActivityResult: user cancelled")
                }
            }
        }

    }

    companion object {
        private const val AUTOCOMPLETE_REQUEST_CODE = 3001
    }
}