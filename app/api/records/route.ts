import { NextRequest, NextResponse } from "next/server";
import { kv } from "@vercel/kv";

// Disable static rendering for this API route
export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const docId = searchParams.get("docId");

    if (!docId || !docId.trim()) {
      return NextResponse.json({ error: "Missing docId parameter" }, { status: 400 });
    }

    const isKvConfigured = !!process.env.KV_REST_API_URL;
    if (!isKvConfigured) {
      // Return a status indicating KV is not configured, so the frontend knows to use local storage
      return NextResponse.json({ 
        success: true, 
        records: [], 
        isCloud: false,
        message: "Vercel KV is not configured. Falling back to local storage."
      });
    }

    const key = `factbook:records:${docId.trim()}`;
    const records = await kv.get(key);

    return NextResponse.json({
      success: true,
      records: records || [],
      isCloud: true
    });
  } catch (error: any) {
    console.error("Error fetching cloud records:", error);
    return NextResponse.json(
      { error: error?.message || "Failed to fetch cloud records." },
      { status: 500 }
    );
  }
}

export async function DELETE(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const docId = searchParams.get("docId");
    const recordId = searchParams.get("id");

    if (!docId || !docId.trim() || !recordId || !recordId.trim()) {
      return NextResponse.json({ error: "Missing docId or id parameter" }, { status: 400 });
    }

    const isKvConfigured = !!process.env.KV_REST_API_URL;
    if (!isKvConfigured) {
      return NextResponse.json({ error: "Vercel KV is not configured on this server." }, { status: 501 });
    }

    const key = `factbook:records:${docId.trim()}`;
    const records = (await kv.get<any[]>(key)) || [];
    
    const updatedRecords = records.filter((rec: any) => rec.id !== recordId);
    
    await kv.set(key, updatedRecords);

    return NextResponse.json({
      success: true,
      records: updatedRecords
    });
  } catch (error: any) {
    console.error("Error deleting cloud record:", error);
    return NextResponse.json(
      { error: error?.message || "Failed to delete cloud record." },
      { status: 500 }
    );
  }
}
