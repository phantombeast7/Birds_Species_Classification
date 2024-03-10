package com.example.birdsspeciesclassification.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.birdsspeciesclassification.R

class ResultFragment : Fragment() {

    private lateinit var birdImageView: ImageView
    private lateinit var birdNameTextView: TextView
    private lateinit var wikiSummaryTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.activity_result, container, false)
        birdImageView = root.findViewById(R.id.birdImageView)
        birdNameTextView = root.findViewById(R.id.birdNameTextView)
        wikiSummaryTextView = root.findViewById(R.id.wikiSummaryTextView)

        // Get data from arguments bundle
        val birdName = arguments?.getString("bird_name")
        val wikiSummary = arguments?.getString("wiki_summary")
        val wikiImageURL = arguments?.getString("wiki_image_url")

        // Set bird name and wiki summary
        birdNameTextView.text = birdName
        wikiSummaryTextView.text = wikiSummary

        // Load and display image using Glide
        Glide.with(this)
            .load(wikiImageURL)
            .into(birdImageView)

        return root
    }
}

class ResultActivity {

}
