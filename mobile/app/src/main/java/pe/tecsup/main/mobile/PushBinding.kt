package pe.tecsup.main.mobile

/** Only a generic update with the binding of the current login can be shown. */
fun acceptsPush(currentBinding:String?,incomingBinding:String?,event:String?):Boolean =
    !currentBinding.isNullOrBlank() && currentBinding==incomingBinding && event=="MAIN_UPDATE"
