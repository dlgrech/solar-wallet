package com.dgsd.android.solar.send

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import com.dgsd.android.solar.R
import com.dgsd.android.solar.common.modalsheet.extensions.showModalFromErrorMessage
import com.dgsd.android.solar.di.util.parentViewModel
import com.dgsd.android.solar.extensions.onEach
import com.dgsd.android.solar.extensions.performHapticFeedback
import com.dgsd.android.solar.extensions.roundedCorners
import com.dgsd.android.solar.extensions.showSnackbar
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.koin.androidx.viewmodel.ext.android.viewModel

class SendScanQRFragment : Fragment(R.layout.frag_send_scan_qr) {

  private val coordinator by parentViewModel<SendCoordinator>()
  private val viewModel by viewModel<SendScanQRViewModel>()

  private var barcodeScannerView: DecoratedBarcodeView? = null

  private val cameraPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) {
    viewModel.onCameraPermissionStateChanged()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    barcodeScannerView = view.requireViewById(R.id.barcode_scanner)
    val toolbar = view.requireViewById<Toolbar>(R.id.toolbar)
    val permissionsContainer = view.requireViewById<View>(R.id.permission_container)
    val pageInstructions = view.requireViewById<View>(R.id.page_instructions)
    val enterDetailsManually = view.requireViewById<View>(R.id.enter_details_manually)

    toolbar.setNavigationOnClickListener {
      requireActivity().onBackPressed()
    }

    permissionsContainer.setOnClickListener {
      viewModel.onRequestCameraPermissionClicked()
    }

    enterDetailsManually.setOnClickListener {
      viewModel.onEnterDetailsManuallyClicked()
    }

    barcodeScannerView?.setStatusText(null)
    barcodeScannerView?.roundedCorners(
      resources.getDimensionPixelSize(R.dimen.card_radius).toFloat()
    )
    barcodeScannerView?.barcodeView?.addStateListener(
      object : CameraPreview.StateListener {
        override fun previewSized() = Unit
        override fun previewStarted() = Unit
        override fun previewStopped() = Unit
        override fun cameraClosed() = Unit
        override fun cameraError(error: Exception?) {
          viewModel.onCameraStateError()
        }
      }
    )
    barcodeScannerView?.barcodeView?.decodeSingle { result ->
      view.performHapticFeedback()
      viewModel.onBarcodeScanResult(result.text.orEmpty())
    }

    onEach(viewModel.showMissingCameraPermissionsState) {
      barcodeScannerView?.isInvisible = it
      permissionsContainer.isInvisible = !it
      pageInstructions.isInvisible = it

      if (!it) {
        barcodeScannerView?.resume()
      }
    }

    onEach(viewModel.showSystemCameraPermissionPrompt) {
      if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
        startActivity(
          Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", requireContext().packageName, null))
        )
      } else {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
      }
    }

    onEach(viewModel.showCameraError) {
      showModalFromErrorMessage(it)
    }

    onEach(viewModel.showInvalidBarcodeError) {
      showSnackbar(it)
    }

    onEach(viewModel.continueWithSolPayRequest) {
      coordinator.navigateWithSolPayRequest(it)
    }

    onEach(viewModel.navigateToEnterAddress) {
      coordinator.navigateToEnterAmount()
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.onResume()

    if (!viewModel.showMissingCameraPermissionsState.value) {
      barcodeScannerView?.resume()
    }
  }

  override fun onPause() {
    barcodeScannerView?.pause()
    super.onPause()
  }

  override fun onDestroyView() {
    barcodeScannerView = null
    super.onDestroyView()
  }
}