package com.bignerdranch.android.myapplication

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.myapplication.R
import java.io.File
import java.sql.Time
import java.util.*


private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0

private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_TIME = 0

class CrimeFragment: Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var requiresPoliceCheckBox: CheckBox

    ///
    private lateinit var timeButton: Button
    ///

    private lateinit var suspectButton: Button
    private lateinit var callSuspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        requiresPoliceCheckBox = view.findViewById(R.id.crime_requires_police) as CheckBox
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callSuspectButton = view.findViewById(R.id.call_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView
        ///
        timeButton = view.findViewById(R.id.crime_time) as Button
        ///

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                Log.i(TAG, "${crime == null}")
                crime?.let {
                    this.crime = it
                    updateUI()
                }
            }
        )
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        ///
        timeButton.text = crime.time.toString()
        ///
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        requiresPoliceCheckBox.apply {
            isChecked = crime.requiresPolice
            jumpDrawablesToCurrentState()
        }
        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }


        if (crime.suspect.isNotEmpty()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()

    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                crime.title = p0.toString()
            }

            override fun afterTextChanged(p0: Editable?) {
                //TODO("Not yet implemented")
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener {
                    _, isChecked -> crime.isSolved = isChecked
            }
        }

        requiresPoliceCheckBox.apply {
            setOnCheckedChangeListener {
                    _, isChecked -> crime.requiresPolice = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        callSuspectButton.setOnClickListener {

            //
            //  Find contact based on name.
            //

            var phone = ""

            val cr: ContentResolver = requireActivity().contentResolver
            val cursor: Cursor? =  cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                "DISPLAY_NAME = '" + crime.suspect + "'",
                null,
                null
            )
            if (cursor!!.moveToFirst()) {
                val contactId: String =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                //
                //  Get all phone numbers.
                //
                val phones: Cursor? = cr.query(
                    Phone.CONTENT_URI, null,
                    Phone.CONTACT_ID + " = " + contactId, null, null
                )
                while (phones!!.moveToNext()) {
                    val number: String = phones.getString(phones.getColumnIndexOrThrow(Phone.NUMBER))
                    phone = number
                }
                phones.close()
            }
            cursor.close()

            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:" + phone)
            startActivity(intent)
        }

        ///
        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.time).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.requireFragmentManager(), DIALOG_TIME)
            }
        }
        ///
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(time: Time) {
        crime.date.hours =time.hours
        crime.date.minutes =time.minutes
        crime.time = time
        updateUI()
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }

    private fun updatePhotoView() {
        /*
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription =
                getString(R.string.crime_photo_image_description)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription =
                getString(R.string.crime_photo_no_image_description)
        }
        */

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri = data.data!!
                //crime.uri = contactUri.toString()
                //Log.d("test",crime.uri)
                // Specify which fields you want your query to return values for.
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                  // Perform your query - the contactUri is like a "where" clause here

                val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
                cursor?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Pull out the first column of the first row of data -
                    // that is your suspect's name.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
/*
                val queryFields2 = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val cursor2 = requireActivity().contentResolver
                    .query(contactUri, queryFields2, null, null, null)
                cursor2?.use {
                    // Verify cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }

                    // Pull out the first column of the first row of data -
                    // that is your suspect's name.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.uri = suspect
                    Log.d("test",suspect)
                    crimeDetailViewModel.saveCrime(crime)
                    callSuspectButton.text = suspect
                }
*/


            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                updatePhotoView()
            }
        }
    }

/*
    var selectedName = ""
    var selectedPhone = ""

    private fun selectPhone(name: String, phone:String){
        selectedName = name
        selectedPhone = phone
        crime.numPhone = phone
        updateUI()
    }



    private inner class PhoneHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private val name: TextView = itemView.findViewById(R.id.name)
        private val phone: TextView = itemView.findViewById(R.id.phone)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(nam: String, phn: String) {
            name.text = nam
            phone.text = phn
        }

        override fun onClick(v: View) {
            selectPhone(name.text.toString(), phone.text.toString())
        }
    }

    private inner class PhoneAdapter(var Names: List<String>, var Phones: List<String>)
        : RecyclerView.Adapter<PhoneHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : PhoneHolder {
            val view = layoutInflater.inflate(if (viewType == 1) R.layout.fragment_act_phone else R.layout.fragment_phone, parent, false)
            return PhoneHolder(view)
        }

        override fun getItemViewType(position: Int): Int {
            return if (selectedPhone == Phones[position]) 1 else 0
        }
        override fun getItemCount()= Names.size

        override fun onBindViewHolder(holder: PhoneHolder, position: Int) {
            holder.bind(Names[position], Phones[position])
        }

        /*fun bind(crime: Crime){
            this.crime = crime
        }*/
    }


*/


}