package com.example.task_043_2

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.task_043_2.databinding.ActivityMainBinding
import android.Manifest
import android.content.ContentProviderOperation
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.RawContacts
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var contactModelList: MutableList<ContactModel>? = null

    val thisContext = this

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED)
        {
            permissionContact.launch(Manifest.permission.READ_CONTACTS)
        } else {
            getContacts()
        }

        setSupportActionBar(binding.toolBarMain)
        title = "Мои контакты."
    }

    override fun onResume() {
        super.onResume()
        binding.addBTN.setOnClickListener{
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED)
            {
                permissionWriteContact.launch(Manifest.permission.WRITE_CONTACTS)
            } else {
                addContact()
                getContacts()
            }
        }
    }

    private fun addContact() {
        val newContactName = binding.newContactNameET.text.toString()
        val newContactPhone = binding.newContactPhoneET.text.toString()
        val listCPO = ArrayList<ContentProviderOperation>()

        listCPO.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build()
        )

        listCPO.add (
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, newContactName)
                .build()
        )

        listCPO.add (
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, newContactPhone)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build()
        )

        Toast.makeText(
            this@MainActivity,
            "$newContactName добавлен в список контактов",
            Toast.LENGTH_SHORT
        ).show()

        try {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, listCPO)
        } catch (e: Exception) {
            Log.e("Exception ", e.message!!)
        }
        binding.newContactNameET.setText("")
        binding.newContactPhoneET.setText("")
    }

    @SuppressLint("Range")
    private fun getContacts() {
        contactModelList = ArrayList()
        val phones = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        while (phones!!.moveToNext()) {
            val name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val contactModel = ContactModel(name, phoneNumber)
            contactModelList?.add(contactModel)
        }
        phones.close()

        binding.recyclerViewRV.layoutManager = LinearLayoutManager(this)

        val adapter = CustomAdapter(contactModelList!!)

        adapter.setOnContactPhoneClickListener( object :
            CustomAdapter.OnContactPhoneClickListener {
            override fun onContactPhoneClick(contact: ContactModel, position: Int) {
                val contact = (contactModelList as ArrayList<ContactModel>)[position]
                val number = contact.phone

                if (ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    permissionOfCall.launch(Manifest.permission.CALL_PHONE)
                } else {
                    callTheNumber(number)
                }
            }
        })

        adapter.setOnContactSmsClickListener( object :
            CustomAdapter.OnContactSmsClickListener {
            override fun onContactSmsClick(contact: ContactModel, position: Int) {
                val contact = (contactModelList as ArrayList<ContactModel>)[position]

                if (ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.SEND_SMS) !=
                    PackageManager.PERMISSION_GRANTED)
                {
                    permissionOfSMS.launch(Manifest.permission.SEND_SMS)
                } else {
                    val intent = Intent(this@MainActivity, SmsActivity::class.java)
                    intent.putExtra("contact", contact)
                    startActivity(intent)
                }
            }
        })

        binding.recyclerViewRV.adapter = adapter
        binding.recyclerViewRV.setHasFixedSize(true)
    }

    private fun callTheNumber(number: String?) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        startActivity(intent)
    }

    private val permissionOfCall = registerForActivityResult(ActivityResultContracts.RequestPermission())
    {
        isGranted ->
        if (isGranted) {
            Toast.makeText(this@MainActivity, "Получен доступ к звонкам.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "В разрешении отказано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionContact = registerForActivityResult(ActivityResultContracts.RequestPermission())
    {
        isGranted ->
        if (isGranted) {
            Toast.makeText(this@MainActivity, "Получен доступ к контактам.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "В разрешении отказано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionOfSMS = registerForActivityResult(ActivityResultContracts.RequestPermission())
    {
        isGranted ->
        if (isGranted) {
            Toast.makeText(this@MainActivity, "Получен доступ к SMS.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "В разрешении отказано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionWriteContact = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
            isGranted ->
        if (isGranted) {
            Toast.makeText(this@MainActivity, "Получен доступ к записи контактов.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "В разрешении отказано.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return  true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.searchActivity->{
                val intent = Intent(this@MainActivity, SearchContactsActivity::class.java)
                startActivity(intent)
            }
            R.id.exitMenuMain->{
                moveTaskToBack(true);
                exitProcess(-1)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}