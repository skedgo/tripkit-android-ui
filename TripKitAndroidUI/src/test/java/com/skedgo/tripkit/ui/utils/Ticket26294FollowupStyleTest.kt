package com.skedgo.tripkit.ui.utils

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.skedgo.tripkit.account.data.AppColor
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.databinding.TripResultListFragmentBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[33])
class Ticket26294FollowupStyleTest {
    private fun binding(night:Boolean,scale:Float=1f):TripResultListFragmentBinding {
        val app=ApplicationProvider.getApplicationContext<android.content.Context>()
        val c=Configuration(app.resources.configuration).apply{fontScale=scale;uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or if(night)32 else 16}
        DynamicAppColor.initAppColors(AppColor(red=95,green=67,blue=154),AppColor(255,255,255),AppColor(red=95,green=67,blue=154))
        return TripResultListFragmentBinding.inflate(LayoutInflater.from(ContextThemeWrapper(app.createConfigurationContext(c),R.style.AppTheme))).apply{executePendingBindings();transportsLayout.visibility=View.VISIBLE}
    }
    private fun background(v:View):Bitmap=Bitmap.createBitmap(320,128,Bitmap.Config.ARGB_8888).also {v.background.setBounds(0,0,320,128);v.background.draw(Canvas(it))}
    @Test fun `departure and Transport share outlined runtime treatment in both themes`() {
        for(night in listOf(false,true)) {
            val b=binding(night);val a=b.leaveNowLayout;val t=b.transportsLayout
            assertEquals("Same background tint treatment",a.backgroundTintList,t.backgroundTintList)
            assertEquals("Same foreground treatment",(a.getChildAt(0) as TextView).currentTextColor,(t.getChildAt(0) as TextView).currentTextColor)
            assertTrue("Same outline fill stroke and corner rendering",background(a).sameAs(background(t)))
            assertEquals(a.paddingLeft,t.paddingLeft);assertEquals(a.paddingRight,t.paddingRight);assertEquals(a.paddingTop,t.paddingTop);assertEquals(a.paddingBottom,t.paddingBottom)
            assertEquals(a.minimumHeight,t.minimumHeight)
            val surface=b.root.context.getColor(R.color.white)
            for(v in listOf(a,t)) {
                val label=v.getChildAt(0) as TextView;val icon=v.getChildAt(1) as ImageView
                assertTrue("Readable outlined action",ColorUtils.calculateContrast(label.currentTextColor,surface)>=4.5)
                assertEquals("Icon uses same semantic foreground",label.currentTextColor,icon.imageTintList!!.defaultColor)
                v.isPressed=true
                assertEquals("Pressed label/icon state",label.currentTextColor,icon.imageTintList!!.getColorForState(icon.drawableState,0))
                assertTrue("Pressed action foreground remains readable",ColorUtils.calculateContrast(label.currentTextColor,surface)>=4.5)
                v.isPressed=false
            }
            assertEquals((a.getChildAt(0) as TextView).currentTextColor,(t.getChildAt(0) as TextView).currentTextColor)
        }
    }
    @Test fun `both controls reference the same reusable component style`() {
        val b=binding(true);val parser=b.root.resources.getLayout(R.layout.trip_result_list_fragment);val styles=mutableMapOf<Int,Int>()
        while(parser.next()!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) if(parser.eventType==org.xmlpull.v1.XmlPullParser.START_TAG) {
            val attrs=android.util.Xml.asAttributeSet(parser);val id=attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android","id",0)
            if(id==R.id.leaveNowLayout || id==R.id.transportsLayout)styles[id]=attrs.styleAttribute
        }
        assertTrue("A shared component style is required",styles[R.id.leaveNowLayout]!=0)
        assertEquals(styles[R.id.leaveNowLayout],styles[R.id.transportsLayout])
    }
    @Test fun `long departure label wraps beside Transport at narrow width and large font`() {
        for(night in listOf(false,true))for(scale in listOf(1f,1.5f,2f)) {
            val b=binding(night,scale);val a=b.leaveNowLayout;val t=b.transportsLayout
            (a.getChildAt(0) as TextView).text="Leave Oct 22, 9:45"
            val header=b.leaveNowAndTransportsHeader;val w=(320*header.resources.displayMetrics.density).toInt()
            header.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));header.layout(0,0,w,header.measuredHeight)
            val ar=android.graphics.Rect(0,0,a.width,a.height);header.offsetDescendantRectToMyCoords(a,ar)
            val tr=android.graphics.Rect(0,0,t.width,t.height);header.offsetDescendantRectToMyCoords(t,tr)
            assertTrue("Both full actions fit: $ar $tr width$w",ar.right<=tr.left && tr.right<=w && ar.left>=0)
            assertTrue(a.width>0 && t.width>0)
            val text=a.getChildAt(0) as TextView
            assertEquals(0,text.layout.getEllipsisCount(text.layout.lineCount-1))
        }
    }
    @Test fun `short departure actions keep intrinsic width and stay adjacent`() {
        for(night in listOf(false,true)) {
            val b=binding(night)
            val date=b.leaveNowLayout
            val transport=b.transportsLayout
            val label=date.getChildAt(0) as TextView
            val icon=date.getChildAt(1) as ImageView
            val header=b.leaveNowAndTransportsHeader
            val width=(400*header.resources.displayMetrics.density).toInt()
            for(text in listOf("Leave now","Leave Oct 22, 9:45")) {
                label.text=text
                header.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                header.layout(0,0,width,header.measuredHeight)
                val intrinsic=kotlin.math.ceil(android.text.Layout.getDesiredWidth(text,label.paint).toDouble()).toInt()+
                    label.compoundPaddingLeft+label.compoundPaddingRight+icon.measuredWidth+date.paddingLeft+date.paddingRight
                assertTrue("Leave must not consume unused row width: actual=${date.width}, intrinsic=$intrinsic",date.width<=intrinsic+1)
                val gap=(date.layoutParams as android.view.ViewGroup.MarginLayoutParams).marginEnd
                assertEquals("Transport stays adjacent",date.right+gap,transport.left)
                assertTrue("Unused horizontal space remains unused",transport.right<width)
            }
        }
    }

}
