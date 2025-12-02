package com.example.reddit.mainpage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import coil.load

import com.example.reddit.R
import com.example.reddit.databinding.ActivityMainPageBinding
import com.example.reddit.login.LoginPage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainPage : AppCompatActivity() {
    private val binding: ActivityMainPageBinding by lazy {
        ActivityMainPageBinding.inflate(layoutInflater)
    }
    private var imageFile: Uri? = null
    private val getImageFromGallery = registerForActivityResult (ActivityResultContracts.GetContent() ) { uri: Uri? ->
        if(uri != null){
            imageFile = uri
        }
    }
    private  val storage= FirebaseStorage.getInstance()
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        binding.getImageBtn.setOnClickListener {
            getImageFromGallery.launch("image/*")
        }
        binding.setImageBtn.setOnClickListener {
            if(imageFile != null){
                binding.imageView.setImageURI(imageFile)
            }
            //imageFile = null

        }
        binding.fromFirebase.setOnClickListener {

            val storageRef = storage.reference.child("start.png")

            storageRef.downloadUrl.addOnSuccessListener { uri ->
                binding.imageView.load(uri){
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_background)
                }


            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Error : ${exception.message}", Toast.LENGTH_SHORT).show()
            }


        }
       binding.sendImage.setOnClickListener {
           if(user!= null ){
               val uniqueName = "${UUID.randomUUID()}.jpg"
               val ref = storage.reference.child("uploads/${user.uid}/$uniqueName")
               ref.putFile(imageFile!!)
                   .addOnSuccessListener {
                       Toast.makeText(this, "Image Uploaded", Toast.LENGTH_SHORT).show()
                   }
           }

       }




    binding.getAll.setOnClickListener {
        loadImagesFromStorage()
    }


binding.logOutBtn.setOnClickListener {
    signOutAndStartOver(this)
}






    }


    // check this function carefully this is a test function , /, uploads dekhna

    private fun loadImagesFromStorage() {
        val folderRef = storage.reference.child("/")
        val container = binding.imageContainer

        folderRef.listAll().addOnSuccessListener { result ->

            for (fileRef in result.items) {
                fileRef.downloadUrl.addOnSuccessListener { uri ->

                    val imageView = ImageView(this)

                    val params = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)
                    params.setMargins(10, 0, 10, 0)
                    imageView.layoutParams = params
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                    imageView.load(uri) {
                        placeholder(R.drawable.ic_launcher_background)
                    }

                    container.addView(imageView)
                }
            }
        }
    }

    fun signOutAndStartOver(context: android.content.Context) {
        val credentialManager = CredentialManager.create(context)

        Firebase.auth.signOut()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            val intent = Intent(context, LoginPage::class.java)
                startActivity(intent)


            } catch (e: Exception) {
                // Handle error (rare)
                e.printStackTrace()
            }
        }
    }
}