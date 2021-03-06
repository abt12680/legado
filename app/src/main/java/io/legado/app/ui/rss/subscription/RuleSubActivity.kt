package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.RuleSub
import io.legado.app.databinding.ActivityRuleSubBinding
import io.legado.app.databinding.DialogRuleSubEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportBookSourceActivity
import io.legado.app.ui.association.ImportReplaceRuleActivity
import io.legado.app.ui.association.ImportRssSourceActivity
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.utils.startActivity
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

/**
 * 规则订阅界面
 */
class RuleSubActivity : BaseActivity<ActivityRuleSubBinding>(),
    RuleSubAdapter.Callback {

    private lateinit var adapter: RuleSubAdapter
    private var liveData: LiveData<List<RuleSub>>? = null

    override fun getViewBinding(): ActivityRuleSubBinding {
        return ActivityRuleSubBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_subscription, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> {
                val order = appDb.ruleSubDao.maxOrder + 1
                editSubscription(RuleSub(customOrder = order))
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        adapter = RuleSubAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initData() {
        liveData?.removeObservers(this)
        liveData = appDb.ruleSubDao.observeAll()
        liveData?.observe(this) {
            binding.tvEmptyMsg.isGone = it.isNotEmpty()
            adapter.setItems(it)
        }
    }

    override fun openSubscription(ruleSub: RuleSub) {
        when (ruleSub.type) {
            0 -> {
                startActivity<ImportBookSourceActivity> {
                    putExtra("source", ruleSub.url)
                }
            }
            1 -> {
                startActivity<ImportRssSourceActivity> {
                    putExtra("source", ruleSub.url)
                }
            }
            2 -> {
                startActivity<ImportReplaceRuleActivity> {
                    putExtra("source", ruleSub.url)
                }
            }
        }
    }

    override fun editSubscription(ruleSub: RuleSub) {
        alert(R.string.rule_subscription) {
            val alertBinding = DialogRuleSubEditBinding.inflate(layoutInflater).apply {
                spType.setSelection(ruleSub.type)
                etName.setText(ruleSub.name)
                etUrl.setText(ruleSub.url)
            }
            customView { alertBinding.root }
            okButton {
                ruleSub.type = alertBinding.spType.selectedItemPosition
                ruleSub.name = alertBinding.etName.text?.toString() ?: ""
                ruleSub.url = alertBinding.etUrl.text?.toString() ?: ""
                launch(IO) {
                    appDb.ruleSubDao.insert(ruleSub)
                }
            }
            cancelButton()
        }.show()
    }

    override fun delSubscription(ruleSub: RuleSub) {
        launch(IO) {
            appDb.ruleSubDao.delete(ruleSub)
        }
    }

    override fun updateSourceSub(vararg ruleSub: RuleSub) {
        launch(IO) {
            appDb.ruleSubDao.update(*ruleSub)
        }
    }

    override fun upOrder() {
        launch(IO) {
            val sourceSubs = appDb.ruleSubDao.all
            for ((index: Int, ruleSub: RuleSub) in sourceSubs.withIndex()) {
                ruleSub.customOrder = index + 1
            }
            appDb.ruleSubDao.update(*sourceSubs.toTypedArray())
        }
    }

}