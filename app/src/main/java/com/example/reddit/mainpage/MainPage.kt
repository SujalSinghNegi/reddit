package com.example.reddit.mainpage

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
import coil.load

import com.example.reddit.R
import com.example.reddit.databinding.ActivityMainPageBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
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









    }


    // check this function carefully this is a test function

    private fun loadImagesFromStorage() {
        val folderRef = storage.reference.child("uploads/${auth.currentUser!!.uid}")
        val container = binding.imageContainer // Reference to the LinearLayout above

        folderRef.listAll().addOnSuccessListener { result ->

            for (fileRef in result.items) {
                fileRef.downloadUrl.addOnSuccessListener { uri ->

                    // 1. Create a new ImageView programmatically
                    val imageView = ImageView(this)

                    // 2. Set size (Width: 150dp, Height: Match Parent)
                    val params = LinearLayout.LayoutParams(400, LinearLayout.LayoutParams.MATCH_PARENT)
                    params.setMargins(10, 0, 10, 0) // Add spacing between images
                    imageView.layoutParams = params
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                    // 3. Load Image using Coil
                    imageView.load(uri) {
                        placeholder(R.drawable.ic_launcher_background)
                    }

                    // 4. Add it to the container
                    container.addView(imageView)
                }
            }
        }
    }
}