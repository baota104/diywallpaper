package com.example.diywallpaper.core.utils.trackings

object TrackingEvents {
    // ================= SPLASH =================
    const val SPLASH_VIEW = "splash_view"
    fun splAdsLibInit(st: String) = "spl_ads_lib_init_$st" // st: done, fail
    fun splMConfigLoad(st: String) = "spl_rm_config_load_$st"
    fun splCmpLoad(st: String) = "spl_cmp_load_$st"
    fun splCmpShow(st: String) = "spl_cmp_show_$st"
    fun splAdsNatPre(scr: String, st: String) = "spl_ads_nat_pre_${scr}_${st}"
    fun splAdsInterLoad(st: String) = "spl_ads_inter_load_$st"
    fun splAdsInterShow(st: String) = "spl_ads_inter_show_$st"
    const val SPLASH_TIMEOUT = "spl_timeout" // Có param time_elapsed
    fun splNext(reason: String) = "spl_next_$reason" // reason: ads_closed, timeout, no_ads

    // ================= LANGUAGE =================
    fun langView(source: String) = "lang_view_$source" // source: spl, set
    fun langAdsNatShow(st: String) = "lang_ads_nat_show_$st"
    fun langSelect(code: String) = "lang_select_$code" // code: en, vi...
    fun langNextClick(code: String) = "lang_next_click_$code"

    // ================= ONBOARD =================
    fun onbView(idx: Int) = "onb_view_p$idx" // idx: 1, 2, 3...
    fun onbAdsNatShow(idx: Int, st: String) = "onb_ads_nat_show_p${idx}_${st}"
    fun onbNextClick(idx: Int) = "onb_next_click_p$idx"
    const val ONB_FINISH_CLICK = "onb_finish_click" // Có param time_spent

    // ================= WELCOME =================
    const val WEL_VIEW = "wel_view"
    fun welAdsNatShow(st: String) = "wel_ads_nat_show_$st"
    const val WEL_NEXT_CLICK = "wel_next_click"

    // ================= PAYWALL =================
    fun pwView(source: String) = "pw_view_$source" // source: onb, set, home
    fun pwPriceFetch(st: String) = "pw_price_fetch_$st"
    fun pwPkgSel(id: String) = "pw_pkg_sel_$id" // id: yr, mo, wk, lf
    fun pwPurClick(id: String) = "pw_pur_click_$id"
    fun pwPurRes(id: String, st: String) = "pw_pur_res_${id}_${st}" // st: done, fail, cancel
    const val PW_CLOSE_CLICK = "pw_close_click" // Có param time_spent

    // ================= HOME =================
    fun homeView(source: String) = "home_view_$source" // source: pw, onb, spl

}