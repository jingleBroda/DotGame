package com.example.presentation.mvvm_single_activity.utils.baseFragment

import android.view.View.OnClickListener
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class BaseFragment(@LayoutRes contentLayoutId:Int):Fragment(contentLayoutId), OnClickListener