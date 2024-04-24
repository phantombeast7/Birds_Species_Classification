package birds.species.birdsspeciesclassification.ui.search


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import birds.species.birdsspeciesclassification.R
import birds.species.birdsspeciesclassification.databinding.FragmentSearchBinding
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageView: ImageView
    private lateinit var fileNameTextView: TextView
    private lateinit var preprocessButton: Button
    private lateinit var requestQueue: RequestQueue

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val CAMERA_PERMISSION_CODE = 100
        const val MY_CUSTOM_TIMEOUT_MS = 10000 // Custom timeout: 10 seconds
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root: View = binding.root

        imageView = binding.imageUploadButton
        fileNameTextView = binding.fileNameTextview
        preprocessButton = binding.preprocessButton

        imageView.setOnClickListener {
            showImageSourceDialog()
        }

        preprocessButton.setOnClickListener {
            val drawable = imageView.drawable
            if (drawable != null && drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                processImage(bitmap)
            } else {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchIcon.setOnClickListener {
            val searchText = binding.searchInput.text.toString()
            if (searchText.isNotEmpty()) {
                processText(searchText)
            } else {
                Toast.makeText(requireContext(), "Please enter text to search", Toast.LENGTH_SHORT).show()
            }
        }

        requestQueue = Volley.newRequestQueue(requireContext())

        // ActivityResultLaunchers setup
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleImageCaptureResult(result.data)
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleImagePickResult(result.data)
            }
        }

        return root
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose an option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> dispatchTakePictureIntent()
                1 -> dispatchPickImageIntent()
            }
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_search_background)
        }

        dialog.show()
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun dispatchPickImageIntent() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(pickIntent)
    }

    private fun handleImageCaptureResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as? Bitmap
        if (imageBitmap != null) {
            val defaultImageName = generateDefaultImageName()
            displayImage(imageBitmap, defaultImageName)
        } else {
            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateDefaultImageName(): String {
        val currentTimeMillis = System.currentTimeMillis()
        return "Image_$currentTimeMillis.jpg"
    }

    private fun getFileNameFromUri(uri: Uri?): String? {
        if (uri == null) return null
        var fileName: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }
        return fileName
    }

    private fun handleImagePickResult(data: Intent?) {
        val imageUri = data?.data
        val imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
        val imageName = getFileNameFromUri(imageUri)
        displayImage(imageBitmap, imageName)

        // Update the fileNameTextView
        fileNameTextView.text = imageName ?: "Image.jpg"
    }

    private fun displayImage(bitmap: Bitmap?, imageName: String?) {
        bitmap?.let {
            imageView.setImageBitmap(bitmap)
            fileNameTextView.text = imageName ?: "Image.jpg"
        }
    }

    private fun processText(searchText: String) {
        val url = "https://nice-rose-cockroach.cyclic.app/bird_info"

        val jsonObject = JSONObject().apply {
            put("bird_name", searchText)
        }
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.show()

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            { response ->
                progressDialog.dismiss()
                try {
                    val birdName = response.getString("bird_name")
                    val summary = response.getString("summary")
                    val imageUrl = response.getString("image_url")
                    startResultActivity(birdName, summary, imageUrl)
                } catch (e: JSONException) {
                    Toast.makeText(requireContext(), "Error processing text", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            },
            { error ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error processing text: ${error.message}", Toast.LENGTH_SHORT).show()
                error.printStackTrace()
            }
        )

        // Set custom retry policy with increased timeout
        request.retryPolicy = DefaultRetryPolicy(
            MY_CUSTOM_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        requestQueue.add(request)
    }

    private fun processImage(bitmap: Bitmap) {
        // 1. Show the loading dialog immediately
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        lifecycleScope.launch(Dispatchers.IO) { // 2. Perform work on background thread

            try {
                // 3. Image compression
                val compressionQuality = 80
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()

                // 4. Base64 encoding
                val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

                // 5. API Call (Using your existing code structure)
                val url = "https://nice-rose-cockroach.cyclic.app/bird_info"
                val jsonObject = JSONObject().apply {
                    put("image", encodedImage)
                }

                val request = JsonObjectRequest(
                    Request.Method.POST, url, jsonObject,
                    { response ->
                        progressDialog.dismiss()
                        if (response != null) {
                            try {
                                val birdName = response.optString("bird_name")
                                val summary = response.optString("summary")
                                val imageUrl = response.optString("image_url")
                                if (birdName.isNotEmpty() && summary.isNotEmpty() && imageUrl.isNotEmpty()) {
                                    startResultActivity(birdName, summary, imageUrl)
                                } else {
                                    Toast.makeText(requireContext(), "Incomplete response received", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: JSONException) {
                                Toast.makeText(requireContext(), "Error processing server response", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Empty response received", Toast.LENGTH_SHORT).show()
                        }
                    },
                    { error ->
                        progressDialog.dismiss()
                        val errorMessage = "Error processing image: ${error.message}"
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        error.printStackTrace()
                    }
                )

                // Set custom retry policy with the desired timeout
                request.retryPolicy = DefaultRetryPolicy(
                    MY_CUSTOM_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                )

                // Add the request to the request queue
                requestQueue.add(request)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // Ensure progressDialog is dismissed, even on network error
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            }
        }
    }





    private fun startResultActivity(birdName: String, summary: String, imageUrl: String) {
        try {
            val intent = Intent(requireContext(), ResultActivity::class.java).apply {
                putExtra("bird_name", birdName)
                putExtra("summary", summary)
                putExtra("image_url", imageUrl)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error starting result activity: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
