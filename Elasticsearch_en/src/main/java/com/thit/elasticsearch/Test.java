package com.thit.elasticsearch;

import java.util.List;

import com.thit.elasticsearch.orcldb.DbOperation;

public class Test {
	private volatile int common;
	public  void seemonitor(){
		synchronized(this){
			for(int i=0;i<1000000;i++){
				common++;
			}
		}
	}
	
	public static void main(String[] args) {
		Test test = new Test();
		MyThread my =test.new MyThread();
		Thread t1 = new Thread(my);
		Thread t2 = new Thread(my);
		long start = System.currentTimeMillis();
		t1.start();
		t2.start();
		
		
//		for(int i=0;i<5;i++){
//			new Thread(()->test.seemonitor()).start();
//		}
//		test.seemonitor();
		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		System.out.println("执行时间："+(end-start-20));
		System.out.println(test.common);
	}
	
	class MyThread implements Runnable{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			seemonitor();
		}
		
	}
	
//	public static void main(String[] args) {
//		//鏁扮粍瀹氫箟鍧愭爣鍒楄〃
//		int[][] zb = {{1,1,3},{2,5,2},{3,1,3},{4,1,4},{1,2,5}};
//		//
//		for (int i = 0; i < zb.length; i++) {
//			boolean flag = true;
//			/**********鍧愭爣鎵撳嵃**************/
//			System.out.print("绗�"+(i+1)+"涓瘮杈冪殑鍧愭爣锛�");
//			System.out.print("(");
//			for(int k : zb[i]){
//				System.out.print(k);
//			}
//			System.out.print(") ");
//			System.out.println();
//			/**********鍧愭爣鎵撳嵃**************/
//			//
//			int a = 2;
//			for (int j = 0; j < zb.length; j++) {
//				//棣栧厛鎺掗櫎涓庤嚜韬殑姣旇緝
//				if(i!=j){
//					if((zb[i][0]-zb[j][0])*(zb[i][0]-zb[j][0])+(zb[i][1]-zb[j][1])*(zb[i][1]-zb[j][1])+(zb[i][2]-zb[j][2])*(zb[i][2]-zb[j][2])>2*2){
//						System.out.print(" 涓庡潗鏍�(");
//						for(int k : zb[j]){
//							System.out.print(k);
//						}
//						System.out.print(") 鐩告瘮璺濈澶т簬2");
//						System.out.println();
//					}else{
//						flag = false;
//					}
//				}
//				
//			}
//			if(flag){
//				System.out.print("鍧愭爣(");
//				for(int k : zb[i]){
//					System.out.print(k);
//				}
//				System.out.print(") 绗﹀悎鏉′欢");
//				System.out.println();
//				System.out.println("鎵ц闇�瑕佹墽琛岀殑绋嬪簭");
//			}
//		}
// 	}
	
	public synchronized void monitor(){
		int i = 2;
		int j = 1;
		int k = i+j;
	}
}
