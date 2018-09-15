package cn.lonsun.staticcenter.generate.tag.impl.video;

import cn.lonsun.base.anno.DbInject;
import cn.lonsun.common.util.AppUtil;
import cn.lonsun.content.internal.dao.IBaseContentDao;
import cn.lonsun.content.internal.entity.BaseContentEO;
import cn.lonsun.content.vo.VideoNewsVO;
import cn.lonsun.core.base.entity.AMockEntity;
import cn.lonsun.core.util.Pagination;
import cn.lonsun.core.util.SqlUtil;
import cn.lonsun.staticcenter.generate.GenerateConstant;
import cn.lonsun.staticcenter.generate.tag.impl.article.ArticleNewsPageListBeanService;
import cn.lonsun.staticcenter.generate.thread.Context;
import cn.lonsun.staticcenter.generate.thread.ContextHolder;
import cn.lonsun.staticcenter.generate.util.AssertUtil;
import cn.lonsun.staticcenter.generate.util.PathUtil;
import cn.lonsun.util.ModelConfigUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <br/>
 *
 * @author wangss <br/>
 * @version v1.0 <br/>
 * @date 2015-12-12<br/>
 */
@Component
public class VideoNewsPageListBeanService extends ArticleNewsPageListBeanService {
    @DbInject("baseContent")
    private IBaseContentDao baseContentDao;

    @Override
    public Object getObject(JSONObject paramObj) {
        Context context = ContextHolder.getContext();
        String strSiteId = context.getParamMap().get("siteId");
        Long siteId = null;
        if (StringUtils.isEmpty(strSiteId)) {
            siteId = context.getSiteId();
        } else {
            siteId = Long.valueOf(strSiteId);
        }
        AssertUtil.isEmpty(siteId, "站点id不能为空！");
        Long[] columnIds = super.getQueryColumnIdByChild(paramObj, BaseContentEO.TypeCode.videoNews.toString());
        AssertUtil.isEmpty(columnIds, "栏目不能为空！");
        Integer pageSize = paramObj.getInteger(GenerateConstant.PAGE_SIZE);
        Long pageIndex = context.getPageIndex() == null ? 0 : (context.getPageIndex() - 1);
        Boolean isImg = paramObj.getBoolean("isImg");// 是否含有图片
        // 查询
        Integer size = columnIds.length;
        Map<String, Object> paramMap = new HashMap<String, Object>();
        StringBuffer hql = new StringBuffer();
        hql.append(" from BaseContentEO c where c.siteId = :siteId and c.isPublish = :isPublish ");
        hql.append(" and c.columnId ").append(size == 1 ? " = :columnIds" : " in (:columnIds) ");
        if (isImg != null && isImg == true) {// 查询有图片的
            hql.append(" and (c.imageLink != '' OR c.imageLink IS NOT NULL) ");
        }
        // 搜索关键字
        String keyWords = context.getParamMap().get("SearchWords");
        if (StringUtils.isNotEmpty(keyWords)) {
            hql.append(" and c.title like '%" + SqlUtil.prepareParam4Query(keyWords) + "%' ");
        }
        // 排序依第一个栏目为准
        String order = ModelConfigUtil.getOrderByHql(columnIds[0], siteId, BaseContentEO.TypeCode.videoNews.toString());
        hql.append(" and c.recordStatus = :recordStatus ").append(order);
        paramMap.put("siteId", siteId);
        paramMap.put("isPublish", 1);// 发布
        paramMap.put("columnIds", size == 1 ? columnIds[0] : columnIds);
        paramMap.put("recordStatus", AMockEntity.RecordStatus.Normal.toString());

        Pagination pagination = baseContentDao.getPagination(pageIndex, pageSize, hql.toString(), paramMap);
         List<BaseContentEO> contentEOs =(List<BaseContentEO>) pagination.getData();
        List<VideoNewsVO> videoNewsVOs =new ArrayList<VideoNewsVO>();
        for (BaseContentEO eo: contentEOs){
            VideoNewsVO vo =new VideoNewsVO();
            AppUtil.copyProperties(vo,eo);
           if(!AppUtil.isEmpty(eo.getImageLink())){
               if(!AppUtil.isEmpty(eo.getEditor())){
                   String path = PathUtil.getPathConfig().getFileServerPath();
                   if(eo.getImageLink().contains("/")){
                       path ="";
                   }
                   if(eo.getImageLink().contains(".")){
                       path = PathUtil.getPathConfig().getFileServerNamePath();
                   }
                   vo.setImageLink(path.replace("/mongo1/","").replace("/mongo/","")+eo.getImageLink());
               }
               else {
                   vo.setImageLink( PathUtil.getUrl(eo.getImageLink()));
               }

           }

            videoNewsVOs.add(vo);

        }
        pagination.setData(videoNewsVOs);

        return pagination;
    }
}