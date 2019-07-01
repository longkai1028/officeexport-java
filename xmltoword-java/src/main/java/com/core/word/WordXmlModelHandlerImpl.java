package com.core.word;


import com.core.utils.FileUtils;
import com.core.utils.StringUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

/**
 * @Auther: SunBC
 * @Date: 2019/6/18 11:09
 * @Description:
 */
public class WordXmlModelHandlerImpl implements XmlModelHandler{
    @Override
    public  void VerifyModel(String xmlPath) throws Exception {
        String  errorInfo = "";
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(xmlPath));
        Element rootElement = document.getRootElement();
        //校验表格
        List tableRowList = document.selectNodes("//w:tbl/w:tr");
        for (int i = 0; i < tableRowList.size(); i++) {
            String tableRowStr = "";
            Node node = (Node)tableRowList.get(i);
            //验证[] # 是否有效
            List TextNodeList = node.selectNodes(".//w:t");
            for (int j = 0; j < TextNodeList.size(); j++) {
                Node TextNode = (Node)TextNodeList.get(j);
                String text = TextNode.getText();
                tableRowStr += StringUtil.removeInvisibleChar(text);
            }
            errorInfo =  XmlParserUtils.VarifySyntax(tableRowStr);
            if (errorInfo.length() != 0) throw new SyntaxException(errorInfo);
        }

        //校验段落
        List ParagList = document.selectNodes(".//wx:sect/w:p");
        StringBuilder wpStr = new StringBuilder();
        for (int i = 0; i < ParagList.size(); i++) {
            Node node = (Node)ParagList.get(i);
            List TextNodeList = node.selectNodes(".//w:t");
            for (int j = 0; j < TextNodeList.size(); j++) {
                Node TextNode = (Node)TextNodeList.get(j);
                String text = TextNode.getText();
                wpStr.append(StringUtil.removeInvisibleChar(text));
            }
        }
        errorInfo= XmlParserUtils.VarifySyntax(wpStr.toString());
        if (errorInfo.length() != 0) throw new SyntaxException(errorInfo);
        return ;
    }

    @Override
    public String ConverToFreemaker(String xmlPath) throws  Exception{
        XMLWriter writer = null;
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(new File(xmlPath));
            List list = document.selectNodes("//w:p");
            for (int i = 0; i <list.size() ; i++) {
                Node WPNode = (Node)list.get(i);
                List WTList = WPNode.selectNodes(".//w:t");
                String textTotal = "";
                Node WTNodeNew = null;
                for (int j = 0; j < WTList.size(); j++) {
                    WTNodeNew = (Node)WTList.get(j);
                    String text = WTNodeNew.getText();
                    textTotal  += text;
                    //可避免无占位符的段落
                    if(XmlParserUtils.ContainPlaceHolder(textTotal))WTNodeNew.setText("");
                    else textTotal = "";
                }
                if (!"".equals(textTotal)) WTNodeNew.setText(textTotal);
            }
            //转换[[ 到list标签
            XmlParserUtils.DoubleBracketToListConversion(document);
            //转换[ 到list标签
            XmlParserUtils.BracketToListConversion(document);
            String xmlFtlPath = xmlPath.replace(".xml", ".ftl");
            FileWriter fileWiter = new FileWriter(xmlFtlPath);
            writer = new XMLWriter(fileWiter);
            writer.write( document );
            return xmlFtlPath;
        }catch (Exception e){
            throw e;
        }finally {
            if (writer != null )
                writer.close();
        }
    }

    @Override
    public void XmlPlaceHolderHandler(String xmlFtlPath) throws Exception{
        XMLWriter writer = null;
        FileOutputStream out = null;
        try {
            String xmModelStr = FileUtils.readToStringByFilepath(xmlFtlPath);
            xmModelStr = XmlParserUtils.IfTagHandle(xmModelStr);
            xmModelStr = XmlParserUtils.ListTagHandle(xmModelStr);
            xmModelStr = XmlParserUtils.BraceTagHandle(xmModelStr);
            out = new FileOutputStream(xmlFtlPath);
            out.write(xmModelStr.getBytes());
            out.flush();
        }catch (Exception e){
            throw e;
        }finally {
            if(out != null){
                out.close();
            }
        }
    }



    public String WordXmlModelHandle(String xmlPath) throws Exception{
        VerifyModel(xmlPath);
        String xmlFtlpath = ConverToFreemaker(xmlPath);
        XmlPlaceHolderHandler(xmlFtlpath);
        return xmlFtlpath;
    }
}