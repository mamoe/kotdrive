# kotdrive

OneDrive API in Kotlin

now supported: 
<li>
Upload File(15G max) to OneDrive
</li>


```kotlin

val driver = oneDriveWorker {
        object : AuthProvider {
            override suspend fun getAccessToken(): AccessToken {
                return AccessToken("bearer", TODO())//ways to get ur token, cache is needed
            }
            //also you can set the API url to use business API index
        }
    }


    try{
        driver.tryNTimes(2) {//try 2 times before upload failed
            this.upload(ConflictBehavior.REPLACE,"mirai/plugins/Test.md", File(System.getProperty("user.dir") + "/README.md"))
        }
        println("Upload Success")
    }catch (e:Exception){
        println("Failed to upload")
        e.printStackTrace()
    }

```