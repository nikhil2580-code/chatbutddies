package com.nikhilkhairnar.chatbuddies.fragments


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.nikhilkhairnar.chatbuddies.R
import com.nikhilkhairnar.chatbuddies.activities.ChatListActivity
import com.nikhilkhairnar.chatbuddies.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()



        binding.authButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Email and Password must not be empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
         binding.progressBar.visibility = View.VISIBLE
            binding.authButton.isEnabled = false
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.VISIBLE
                    binding.authButton.isEnabled = true
                    if (task.isSuccessful) {
                        if (isAdded) {
                            val intent = Intent(requireContext(), ChatListActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    } else {
                        val builder = android.app.AlertDialog.Builder(requireContext())
                        builder.setTitle("Login Failed")
                        builder.setMessage("Incorrect email or password. Please try again.")
                        builder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        builder.show()


                    }
                }
        }

        binding.toggleText.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }

//    override fun onStart() {
//        super.onStart()
//        if (FirebaseAuth.getInstance().currentUser != null) {
//            val intent = Intent(requireContext(), ChatListActivity::class.java)
//            startActivity(intent)
//            requireActivity().finish()
//        }
//    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
