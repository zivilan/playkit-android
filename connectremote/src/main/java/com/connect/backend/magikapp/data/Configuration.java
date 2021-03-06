package com.connect.backend.magikapp.data;

import android.graphics.Color;
import android.support.annotation.StringDef;

import com.connect.backend.BaseResult;

import java.lang.annotation.Retention;
import java.util.ArrayList;

import static com.connect.backend.magikapp.data.Configuration.ColorType.Accent;
import static com.connect.backend.magikapp.data.Configuration.ColorType.Primary;
import static com.connect.backend.magikapp.data.Configuration.ColorType.Secondary;
import static com.connect.backend.magikapp.data.Configuration.GroupType.Grid;
import static com.connect.backend.magikapp.data.Configuration.GroupType.Slider;
import static com.connect.backend.magikapp.data.Configuration.ThemeType.Festival;
import static com.connect.backend.magikapp.data.Configuration.ThemeType.Food;
import static com.connect.backend.magikapp.data.Configuration.ThemeType.Party;
import static com.connect.backend.magikapp.data.Configuration.ThemeType.Sport;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by tehilarozin on 01/01/2017.
 */

public class Configuration extends BaseResult {

    ThemeConf theme;
    MetadataConf metadata;
    ContentConf content;
    BillingConf billing;


    public String getIcon(){
        return metadata.icon;
    }

    public String getLogo(){
        return metadata.logo;
    }

    public String getName(){
        return metadata.name;
    }

    public int getChannelId(){
        return metadata != null ? metadata.channelId : 384719;
    }

    public String getPrimaryClr(){
        return theme.color.primary;
    }

    public int getPrimary(){
        return Color.parseColor(getPrimaryClr());
    }

    public int getSecondary(){
        return Color.parseColor(getSecondaryClr());
    }

    public int getAccent(){
        return Color.parseColor(getAccentClr());
    }


    public String getSecondaryClr(){
        return theme.color.secondary;
    }

    public String getAccentClr(){
        return theme.color.accent;
    }

    public @ThemeType String getThemeType(){
        return theme != null ? theme.type : null;
    }

    public ArrayList<MenuItemConf> getMenu(){
        return content.menu;
    }

    public MenuItemConf getMenuItem(int idx){
        return (content.menu != null && idx >= 0 && content.menu.size() > idx) ? content.menu.get(idx) : null;
    }

    public String getSplashVideo() {
        return metadata != null ? metadata.splashScreen : null;
    }

    public String getSplashImage() {
        return metadata != null ? metadata.splashScreenImage : null;
    }

    public String getFontFamily() {
        return theme != null && theme.font != null? theme.font.fontFamily : null;
    }


    private class ThemeConf {
        @ThemeType String type;
        ColorConf color;
        FontConf font;
    }

    private class ColorConf {
        String primary;
        String secondary;
        String accent;
    }

    private class FontConf {
        String fontFamily;
    }

    private class MetadataConf {
        int channelId;
        String icon; //image url
        String logo; //image url
        String name;
        String splashScreen; //video url
        String splashScreenImage; //image url
    }

    private class ContentConf {
        ArrayList<MenuItemConf> menu;
    }

    private class BillingConf {
        String applicationId;
    }

    public class MenuItemConf {
        public String name;
        public String title;
        public ArrayList<GroupConf> groups;
    }

    public ArrayList<GroupConf> getMenuContent(String id){
        for(MenuItemConf menuItemConf : content.menu){
            if(menuItemConf.name.equals(id)){
                return menuItemConf.groups;
            }
        }
        return null;
    }

    class GroupConf{
        String title;
        @GroupType String type;
        int contentLimit;
    }

    @Retention(SOURCE)
    @StringDef(value = {Slider, Grid})
    public @interface GroupType {
        String Slider = "slider";
        String Grid = "grid";
    }

    @Retention(SOURCE)
    @StringDef(value = {Food,Party,Festival,Sport})
    public @interface ThemeType {
        String Food = "food";
        String Party = "party";
        String Festival = "festival";
        String Sport = "sport";
    }

    @Retention(SOURCE)
    @StringDef(value = {Primary, Secondary, Accent})
    public @interface ColorType {
        String Primary = "primary";
        String Secondary = "secondary";
        String Accent = "accent";
    }



}
