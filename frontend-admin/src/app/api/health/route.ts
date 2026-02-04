import { NextResponse } from 'next/server';

export async function GET() {
    return NextResponse.json(
        {
            status: 'healthy',
            timestamp: new Date().toISOString(),
            service: 'USSD Admin Frontend'
        },
        { status: 200 }
    );
}
