package org.example.entity;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
public record RustBean<T>(int code , T data , String message) {
    public  static <T>  RustBean<T> success(T data){
        return  new RustBean<>(200,data,"请求成功");
    }
    public  static  <T> RustBean<T> success(){
        return  success( null );
    }
    public  static  <T> RustBean<T> failure(int code,String message){
        return  new RustBean<>(code,null,message);
    }
    public  static  <T> RustBean<T> unauthorized(String message){
        return  new RustBean<>(401,null,message);
    }
    public  static  <T> RustBean<T> forbidden(String message){
        return  new RustBean<>(403,null,message);
    }
    // 将数据转化成Json格式
    public String asJsonString(){
        return    JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }
}