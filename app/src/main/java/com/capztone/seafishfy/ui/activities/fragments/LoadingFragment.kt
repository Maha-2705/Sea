// LoadingFragment.kt
package com.capztone.seafishfy.ui.activities.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capztone.seafishfy.R

class LoadingFragment : Fragment() {

    private val loadingTimeMillis: Long = 1000 // Time in milliseconds (1000 ms = 1 second)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handler to post a delayed action to navigate to HomeFragment
        Handler().postDelayed({
            findNavController().navigate(R.id.action_loadingFragment_to_HomeFragment)
        }, loadingTimeMillis)
    }
}
