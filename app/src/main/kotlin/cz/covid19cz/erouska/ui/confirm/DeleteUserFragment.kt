package cz.covid19cz.erouska.ui.confirm

import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.service.CovidService
import cz.covid19cz.erouska.utils.Auth

class DeleteUserFragment : ConfirmationFragment() {
    override val description by lazy { getString(R.string.delete_user_desc, Auth.getPhoneNumber())}
    override val buttonTextRes = R.string.delete_registration
    override fun confirmedClicked() {
        viewModel.deleteUser()
    }

    override fun doWhenFinished() {
        context?.let {
            it.startService(CovidService.stopService(it, true))
        }
        navigate(R.id.action_nav_delete_user_to_nav_delete_user_success)
    }
}