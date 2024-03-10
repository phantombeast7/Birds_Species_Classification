package com.example.birdsspeciesclassification.ui.search

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.birdsspeciesclassification.R
import com.example.birdsspeciesclassification.databinding.FragmentSearchBinding
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageView: ImageView
    private lateinit var fileNameTextView: TextView
    private lateinit var preprocessButton: Button

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2

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
            // Check if an image has been selected
            val drawable = imageView.drawable
            if (drawable != null && drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                processImage(bitmap)
            } else {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }

        return root
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
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun dispatchPickImageIntent() {
        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also { pickIntent ->
            pickIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(pickIntent, REQUEST_PICK_IMAGE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> handleImageCaptureResult(data)
                REQUEST_PICK_IMAGE -> handleImagePickResult(data)
            }
        }
    }

    private fun handleImageCaptureResult(data: Intent?) {
        val imageBitmap = data?.extras?.get("data") as? Bitmap
        displayImage(imageBitmap)
    }

    private fun handleImagePickResult(data: Intent?) {
        val imageUri = data?.data
        val imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
        displayImage(imageBitmap)
    }

    private fun displayImage(bitmap: Bitmap?) {
        bitmap?.let {
            imageView.setImageBitmap(bitmap)
            fileNameTextView.text = "Image.jpg" // You can replace this with the actual file name
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val url = "https://bird-api-fuh3kecoba-uc.a.run.app" // Updated URL with endpoint

        val requestQueue = Volley.newRequestQueue(requireContext())
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageByteArray = byteArrayOutputStream.toByteArray()

        val request = object : JsonObjectRequest(Request.Method.POST, url, null,
            Response.Listener<JSONObject> { response ->
                try {
                    val birdName = response.getString("predicted_species")
                    val wikiSummary = response.getString("wiki_summary")
                    val wikiImageURL = response.getString("wiki_image_url")
                    startResultActivity(birdName, wikiSummary, wikiImageURL)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show()
            }) {
            override fun getBodyContentType(): String {
                return "application/octet-stream"
            }

            override fun getBody(): ByteArray {
                return imageByteArray
            }
        }
        requestQueue.add(request)
    }

    private fun startResultActivity(birdName: String, wikiSummary: String, wikiImageURL: String) {
        val intent = Intent(context, ResultActivity::class.java).apply {
            putExtra("bird_name", birdName)
            putExtra("wiki_summary", wikiSummary)
            putExtra("wiki_image_url", wikiImageURL)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
