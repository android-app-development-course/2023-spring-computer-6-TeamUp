package com.example.teamup

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.teamup.ViewPagerUI.home.HomeFragment
import com.example.teamup.ViewPagerUI.me.MeFragment
import com.example.teamup.ViewPagerUI.team.TeamFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.hypot


class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: MyPagerAdapter
    private lateinit var anim: ValueAnimator
    private lateinit var circleView: View
    private var UserID = "-1"    // 登录 ID, 没有登录为 -1
    fun isLogin():Boolean = UserID != "-1"

    private var maxRadius = 200.0 // 圆形视图的最大半径
    private var needShrinkAnimation = false // 是否需要执行缩小动画的标志
    private var isFirstLoading = true

    /** 上次点击返回键的时间  */
    private var lastBackPressed: Long = 0

    /** 两次点击的间隔时间  */
    private val QUIT_INTERVAL = 3000

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide() // 隐藏顶部栏

        // 读取内存
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("LoginUserInfo", MODE_PRIVATE)
        UserID = sharedPreferences.getString("id","-1") ?: "-1"

//       btnNewActivity 新建 添加监听事件
        val btnCreateTeam = findViewById<FloatingActionButton>(R.id.btnCreateTeam)
        btnCreateTeam.setOnClickListener{
//            如果没有登录则打开登录提示
            if(!isLogin()) {
                DialogUnLogin(this).show()
            }else {
                // 播放放大动画
                fabScaleAnim(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        needShrinkAnimation = true
                        val CreateTeamIntent = Intent(this@MainActivity,CreateTeamActivity::class.java)
                        startActivity(CreateTeamIntent)
                    }
                })
            }
        }
        viewPager = findViewById(R.id.viewPager)
        adapter = MyPagerAdapter(this)
        viewPager.adapter = adapter

        //设置初始页面为 首页
        viewPager.setCurrentItem(1,false)

//      底部导航栏 nav
        var bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.selectedItemId=R.id.navigation_home //初始底部栏为首页
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_team -> {
                    viewPager.currentItem = 0
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_home -> {
                    viewPager.currentItem = 1
                    return@setOnItemSelectedListener true
                }
                R.id.navigation_me -> {
                    viewPager.currentItem = 2
                    return@setOnItemSelectedListener true
                }
                else -> return@setOnItemSelectedListener false
            }
        }

//        当点击底部栏时，显示对应的 Fragment
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val menuItem = bottomNavigationView.menu.getItem(position)
                menuItem.isChecked = true
            }
        })
    }

    private fun fabScaleAnim(listener: Animator.AnimatorListener? = null) {
        val fab: FloatingActionButton = findViewById(R.id.btnCreateTeam)
        val container: ViewGroup = findViewById(android.R.id.content)

        // 获取 FloatingActionButton 的中心坐标
        val fabX = fab.x + fab.width / 2
        val fabY = fab.y + fab.height / 2

        // 计算最大半径
        maxRadius = hypot(container.width.toDouble(), container.height.toDouble())

        // 创建圆形视图
        circleView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, 0).apply {
                gravity = Gravity.CENTER
            }
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.circle_background)
            container.addView(this)
        }

        // 创建放大动画
        anim = ValueAnimator.ofFloat(0f, maxRadius.toFloat()).apply {
            duration = 500
            interpolator = AccelerateInterpolator()
            addUpdateListener { animation ->
                val radius = animation.animatedValue as Float
                // 设置圆形视图的宽高
                circleView.layoutParams.width = radius.toInt() * 2
                circleView.layoutParams.height = radius.toInt() * 2
                circleView.requestLayout()
                // 设置圆形视图的位置
                circleView.x = fabX - radius
                circleView.y = fabY - radius
            }
            listener?.let { addListener(it) }
        }
        // 开始动画
        anim.start()
    }

    override fun onResume() {
        super.onResume()
        if(isFirstLoading) {
            isFirstLoading = false
        }else{
            overridePendingTransition(0,0)
        }

        if (needShrinkAnimation) {
            // 创建缩小动画
            val anim = ValueAnimator.ofFloat(maxRadius.toFloat(), 0f).apply {
                duration = 500
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    circleView.layoutParams.width = value.toInt()
                    circleView.layoutParams.height = value.toInt()
                    circleView.requestLayout()
                }
            }
            // 启动动画
            anim.start()
            // 将标志重置为 false，下次不会执行动画
            needShrinkAnimation = false
        }
    }



    private class MyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        private val fragments = listOf(TeamFragment(), HomeFragment(), MeFragment())
        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
    }





    //重写onKeyDown()
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() === 0) {
            val backPressed = System.currentTimeMillis()
            if (backPressed - lastBackPressed > QUIT_INTERVAL) {
                lastBackPressed = backPressed
                Toast.makeText(this, "再按一次退出TeamUp", Toast.LENGTH_LONG).show()
            } else {
                finish()
                System.exit(0)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

