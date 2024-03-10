package com.example.birdsspeciesclassification.ui.feedback

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.example.birdsspeciesclassification.R

class FeedbackFragment : Fragment() {

    private lateinit var submitButton: Button
    private lateinit var feedbackEditText: EditText
    private lateinit var radioButtonYes: RadioButton
    private lateinit var emoji1: ImageView
    private lateinit var emoji2: ImageView
    private lateinit var emoji3: ImageView
    private lateinit var emoji4: ImageView
    private lateinit var emoji5: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feedback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        submitButton = view.findViewById(R.id.submitButton)
        feedbackEditText = view.findViewById(R.id.feedbackEditText)
        radioButtonYes = view.findViewById(R.id.radioButtonYes)
        emoji1 = view.findViewById(R.id.emoji1)
        emoji2 = view.findViewById(R.id.emoji2)
        emoji3 = view.findViewById(R.id.emoji3)
        emoji4 = view.findViewById(R.id.emoji4)
        emoji5 = view.findViewById(R.id.emoji5)

        // Add onClickListeners
        emoji1.setOnClickListener { emojiClicked(emoji1) }
        emoji2.setOnClickListener { emojiClicked(emoji2) }
        emoji3.setOnClickListener { emojiClicked(emoji3) }
        emoji4.setOnClickListener { emojiClicked(emoji4) }
        emoji5.setOnClickListener { emojiClicked(emoji5) }

        submitButton.setOnClickListener {
            val feedbackText = feedbackEditText.text.toString()
            val followUp = if (radioButtonYes.isChecked) "Yes" else "No"
            val selectedEmojiRating = determineEmojiRating()

            val feedbackData = FeedbackData(feedbackText, selectedEmojiRating, followUp)
            processFeedback(feedbackData)
        }
    }

    private fun emojiClicked(clickedEmoji: ImageView) {
        resetAllEmojiColors()
        clickedEmoji.setColorFilter(Color.YELLOW)
    }

    private fun resetAllEmojiColors() {
        val emojis = listOf(emoji1, emoji2, emoji3, emoji4, emoji5)
        emojis.forEach { it.clearColorFilter() }
    }

    private fun determineEmojiRating(): Int {
        val emojis = listOf(emoji1, emoji2, emoji3, emoji4, emoji5)
        val yellowColor = ContextCompat.getColor(requireContext(), R.color.yellow) // Replace 'yellow' with your color name
        for ((index, emoji) in emojis.withIndex()) {
            val drawable = emoji.drawable
            if (drawable != null && drawable is ColorDrawable) {
                val color = drawable.color
                if (color == yellowColor) {
                    return index + 1 // Ratings from 1 to 5
                }
            }
        }
        return -1 // Default if no emoji is selected
    }




    private fun processFeedback(feedbackData: FeedbackData) {
        // Here you can handle the collected feedback, for example, send it to a server
    }

    data class FeedbackData(val text: String, val emojiRating: Int, val followUp: String)
}
