package org.sonicx.core.db.backup;

import java.io.File;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDB;
import org.sonicx.common.application.Application;
import org.sonicx.common.application.ApplicationFactory;
import org.sonicx.common.application.SonicxApplicationContext;
import org.sonicx.common.utils.FileUtil;
import org.sonicx.common.utils.PropUtil;
import org.sonicx.core.config.DefaultConfig;
import org.sonicx.core.config.args.Args;
import org.sonicx.core.db.Manager;
import org.sonicx.core.db.ManagerForTest;
import org.sonicx.core.db2.core.RevokingDBWithCachingNewValue;
import org.sonicx.core.db2.core.SnapshotManager;

@Slf4j
public class BackupDbUtilTest {

  static {
    RocksDB.loadLibrary();
  }

  public SonicxApplicationContext context;
  public Application AppT = null;
  public BackupDbUtil dbBackupUtil;
  public Manager dbManager;
  public ManagerForTest mngForTest;
  public String dbPath = "output-BackupDbUtilTest";

  String propPath;
  String bak1Path;
  String bak2Path;
  int frequency;

  @Before
  public void before() {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", "database",
            "--storage-index-directory", "index"
        },
        "config-test-dbbackup.conf"
    );

    context = new SonicxApplicationContext(DefaultConfig.class);
    AppT = ApplicationFactory.create(context);
    dbManager = context.getBean(Manager.class);
    dbBackupUtil = context.getBean(BackupDbUtil.class);
    mngForTest = new ManagerForTest(dbManager);

    //prepare prop.properties
    propPath = dbPath + File.separator + "test_prop.properties";
    bak1Path = dbPath + File.separator + "bak1/database";
    bak2Path = dbPath + File.separator + "bak2/database";
    frequency = 50;
    Args cfgArgs = Args.getInstance();
    cfgArgs.getDbBackupConfig()
        .initArgs(true, propPath, bak1Path, bak2Path, frequency);
    FileUtil.createFileIfNotExists(propPath);
  }

  @After
  public void after() {
    AppT.shutdownServices();
    AppT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testDoBackup() {
    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("11"));
    mngForTest.pushNTestBlock(50);
    List<RevokingDBWithCachingNewValue> alist = ((SnapshotManager) dbBackupUtil.getDb()).getDbs();

    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 50);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE())));

    mngForTest.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 100);
    Assert.assertTrue("11".equals(
        PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE())));

    mngForTest.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 150);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE())));

    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("1"));
    mngForTest.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 200);
    Assert.assertTrue("11".equals(
        PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE())));

    PropUtil.writeProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE(),
        String.valueOf("2"));
    mngForTest.pushNTestBlock(50);
    Assert.assertTrue(dbManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() == 250);
    Assert.assertTrue("22".equals(
        PropUtil.readProperty(propPath, BackupDbUtil.getDB_BACKUP_STATE())));
  }
}