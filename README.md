## Zage Android SDK
The Zage react-native package provides a lightweight react component that makes it easy to implement Zage as a payment method into your react-native application. 

## Author

Zage Inc (tryzage.com)

## License

The Zage Android is available under the MIT license

## Integration 

#### Intro
The Zage Android SDK provides an easy and lightweight way to implement Zage as a payment method into your Android application. To use it, you must have a public and private key pair from Zage, so please ensure that you have received one. 
    
#### Setup 
Add the Zage Android SDK to your project's dependencies. This will look slightly different depending on which build tool you are using (Gradle, Maven, etc). In Gradle, for example you would add this to your module's build.gradle file

```kotlin
dependencies {
    implementation 'com.tryzage:zage-android:1.0.0'
}
```

Then import the Zage class into your file:
```kotlin
import com.tryzage.zageandroid.Zage
```

#### Implementation

In your activity's onCreate method, instantiate an instance of the Zage object as a class variable with your context and public key:

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var zage: Zage;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        zage = Zage(this, "<PUBLIC_KEY>")
    }
}
```
    
Next, in the handler you'll use to open the Zage payment process, call the openPayment method of the Zage object you created and pass in your payment token, onComplete callback, and onExit callback, along with any other functionality you wish to include. Be aware that the onComplete callback will return a serialized version of the JSON object returned by the web hook you used to create the payment token. 

```kotlin
fun openPayment() {    
	zage.openPayment("<PAYMENT_TOKEN>",
        { res ->
            // insert onSuccess functionality here
            println("I completed a payment: $res")
        },
        {
    		// insert onExit functionality here
    		println("I exited a payment")
    	}
	)
	// insert any other functionality you wish to include here 
}
```
And that's it! With just the Zage object and one method, you can integrate Zage into your Android application. Here is an example with all of the pieces in one place:

### Full Implementation Example

```kotlin
package com.example.yourProject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var zage: Zage;
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Instantiate Zage object with context and publicKey
        zage = Zage(this, "<PUBLIC_KEY>")

        // Find button on view
        var payButton: Button = findViewById(R.id.button)
        // Set on click handler for button
        payButton.setOnClickListener {
            tappedButton()
        }
    }

    fun tappedButton() {
        // Open Zage payment flow
        zage.openPayment("<PAYMENT_TOKEN>", // Pass in payment token 
            { res ->
                // Callback for when user completes payment flow
                println("I completed a payment: $res")
            },
            {
                // Callback for when user exits payment flow
                println("I exited a payment")
            }
        )
    }
}
```