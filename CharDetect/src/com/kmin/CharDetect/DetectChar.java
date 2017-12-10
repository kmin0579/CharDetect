package com.kmin.CharDetect;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import org.opencv.core.*;
import org.opencv.android.*;
import org.opencv.imgproc.*;
import android.util.*;
import org.opencv.imgcodecs.*;
import java.util.*;
import android.os.*;

public class DetectChar
{
	private ArrayList<int[]> char_area = new ArrayList<int[]>();//文字区域坐标 int[4]:(x1,x2,y1,y2)
	private MainActivity ui = null;
	private int progress = 0;//进度
	private final int lineThresh = 20;//横向像素阈值
	private final int colThresh = 2;//纵向像素阈值
	public int maxHeight = 50;//最大高度
	public int minHeight = 20;//最小高度
	public int minWidth = 10;//最小宽度
	
	public Bitmap detect(Bitmap src_bmp,MainActivity ma)
	{
		ui = ma;
		Bitmap ret_bmp = Bitmap.createBitmap(src_bmp.getWidth(),src_bmp.getHeight(),Config.RGB_565);
		
		Mat src_rgb_mat = new Mat();
		Mat src_gray_mat = new Mat();
		Mat result_mat = new Mat();
		
		Utils.bitmapToMat(src_bmp,src_rgb_mat);
		Imgproc.cvtColor(src_rgb_mat,src_gray_mat,Imgproc.COLOR_RGB2GRAY);
		
		result_mat = preProc(src_gray_mat);
		disProg(1);
		getArea(result_mat);
		drawArea(src_rgb_mat);
		
		
		Utils.matToBitmap(src_rgb_mat,ret_bmp);
		
		return ret_bmp;
	}
	//预处理，膨胀腐蚀
	private Mat preProc(Mat src_mat)
	{
		Mat ret_mat =new Mat();
		Imgproc.Canny(src_mat,ret_mat,30,30);//提取轮廓
		//Imgproc.threshold(ret_mat,ret_mat,100,255,Imgproc.THRESH_OTSU|Imgproc.THRESH_BINARY_INV);  //二值化
		Mat seY = Mat.ones(1,25,CvType.CV_8UC1);//横向
		Imgproc.dilate(ret_mat,ret_mat,seY);//膨胀
		Imgproc.erode(ret_mat,ret_mat,seY);//腐蚀
		
		Mat seX = Mat.ones(5,1,CvType.CV_8UC1);//纵向
		Imgproc.dilate(ret_mat,ret_mat,seX);
		Imgproc.erode(ret_mat,ret_mat,seX);
		
		return ret_mat;
	}
	
	private void getArea(Mat src_mat)
	{
		scanArea(src_mat);
		int i;
		
		for(i = 0;i < char_area.size();++i)
		{
			int char_up = char_area.get(i)[0];
			int char_down = char_area.get(i)[1];
			int char_left = char_area.get(i)[2];
			int char_right = char_area.get(i)[3];
			if(char_down - char_up < minHeight || char_down - char_up > maxHeight || char_right - char_left < minWidth)
			{
				int[] tm = {-1,-1,-1,-1};
				char_area.set(i,tm);
				
			}
		}
	}
	
	private void scanArea(Mat src_mat)
	{
		//横向
		int i = 0;
		int j = 0;
		int k = 0;
		
		int width = src_mat.width();
		int height = src_mat.height();
		int[] cntLine = new int[height];//一行中的非零像素
		int[] cntCol = new int[width];//一列中的非零像素
		int step = width*height/40;
		int cnt_tmp = 0;
		//统计每一行的非零像素个数
		for(i = 0;i < height;++i)
		{
			cntLine[i] = 0;
			for(j = 0;j < width;++j)
			{
				if(src_mat.get(i,j)[0] > 0)
				{
					++cntLine[i];
				}
				
				++cnt_tmp;
				if(cnt_tmp == step)
				{
					disProg(1);
					cnt_tmp = 0;
				}
			}
		}
		cnt_tmp = 0;
		//找大于阈值的行，得到可能的区域
		boolean lineStart = false;//行区域找到上边界的标志
		int charUp = 0;//上边界
		int charDown = 0;//下边界
		int charLeft = 0;//左边界
		int charRight = 0;//右边界
		boolean colStartFlag = false;//列开始扫描标志
		for(i = 0;i < height;++i)
		{
			if(cntLine[i] > lineThresh && i < height && !lineStart)//区域上边界
			{
				lineStart = true;
				charUp = i;
			}
			else if((cntLine[i] < lineThresh || i == height-1) && lineStart)//下边界
			{
				lineStart = false;
				charDown = i;
				colStartFlag = true;
			}
			if(colStartFlag)//开始扫描列边界
			{
				boolean colStart = false;
				for(j = 0;j < width;++j)//统计区域内列方向非零元素个数
				{
					cntCol[j] = 0;
					for(k = charUp;k < charDown;++k)
					{
						if(src_mat.get(k,j)[0] > 0)
						{
							++cntCol[j];//区域内列方向非零元素个数
						}
					}
				}
				for(j = 0;j < width;++j)//列扫描与行类似
				{
					if(cntCol[j] > colThresh && j < width && !colStart)//区域上边界
					{
						colStart = true;
						charLeft = j;
					}
					else if((cntCol[j] < colThresh || j == width-1) && colStart)//下边界
					{
						colStart = false;
						charRight = j;
						int[] tmp = {charUp,charDown,charLeft,charRight};
						char_area.add(tmp);
					}
				}
			}
			++cnt_tmp;
			if(cnt_tmp == step)
			{
				disProg(1);
				cnt_tmp = 0;
			}
		}
	}
	
	private void drawArea(Mat src_mat)
	{
		int i,k;
		int width = src_mat.width();
		int height = src_mat.height();
		double[] color = new double[src_mat.channels()];
		color[0] = 255;
		for(i = 1;i < src_mat.channels();++i)
		{
			color[i] = 0;
		}
		for(k = 0;k < char_area.size();++k)
		{
			int charUp,charDown,charLeft,charRight;
			
			int[] area_tmp = char_area.get(k);
			charUp = area_tmp[0];
			charDown = area_tmp[1];
			charLeft = area_tmp[2];
			charRight = area_tmp[3];
			if(charUp == -1)continue;
			disProg(1);
			for(i = charUp;i < charDown;++i)
			{
				src_mat.put(i,charLeft,color);
				
				src_mat.put(i,charRight,color);
			}
			disProg(1);
			for(i = charLeft;i < charRight;++i)
			{
				src_mat.put(charUp,i,color);
				src_mat.put(charDown,i,color);
			}
			
			disProg(1);
		}
	}
	
	private void disProg(int rate)//输入是执行某操作占用的整体时间比例0-100
	{
		progress += rate;
		if(progress > 98)progress=98;
		ui.mTask.displayProgress(progress);
	}
}
